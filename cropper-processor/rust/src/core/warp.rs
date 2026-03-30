use crate::core::color::{linear_to_srgb, srgb_to_linear};
use wide::f32x4;

#[cfg(feature = "rayon")]
use rayon::prelude::*;

pub fn perform_warp(
    input: &[u8],
    w: u32,
    h: u32,
    corners: &[f32; 8],
    tw: u32,
    th: u32,
) -> Option<Vec<u8>> {
    if w < 2 || h < 2 || tw == 0 || th == 0 {
        return None;
    }

    let expected_len = w.checked_mul(h)?.checked_mul(4)?.try_into().ok()?;
    if input.len() < expected_len {
        return None;
    }

    let (s0x, s0y) = (corners[0], corners[1]);
    let (s1x, s1y) = (corners[2], corners[3]);
    let (s2x, s2y) = (corners[4], corners[5]);
    let (s3x, s3y) = (corners[6], corners[7]);

    let dx1 = s1x - s2x;
    let dx2 = s3x - s2x;
    let sx = s0x - s1x + s2x - s3x;

    let dy1 = s1y - s2y;
    let dy2 = s3y - s2y;
    let sy = s0y - s1y + s2y - s3y;

    let (a_raw, b_raw, c_mat, d_raw, e_raw, f_mat, g_raw, h_raw) = if sx == 0.0 && sy == 0.0 {
        (
            s1x - s0x,
            s2x - s1x,
            s0x,
            s1y - s0y,
            s2y - s1y,
            s0y,
            0.0,
            0.0,
        )
    } else {
        let det = dx1 * dy2 - dx2 * dy1;
        if det == 0.0 {
            return None; // Invalid quad
        }
        let g = (sx * dy2 - sy * dx2) / det;
        let h_mat = (dx1 * sy - dy1 * sx) / det;
        (
            s1x - s0x + g * s1x,
            s3x - s0x + h_mat * s3x,
            s0x,
            s1y - s0y + g * s1y,
            s3y - s0y + h_mat * s3y,
            s0y,
            g,
            h_mat,
        )
    };

    // Scale to match (tw, th)
    let tw_f = tw as f32;
    let th_f = th as f32;
    let a = a_raw / tw_f;
    let b = b_raw / th_f;
    let d = d_raw / tw_f;
    let e = e_raw / th_f;
    let g = g_raw / tw_f;
    let h_mat = h_raw / th_f;

    let mut out = vec![0u8; (tw * th * 4) as usize];
    let row_len = (tw * 4) as usize;

    let process_row = |y: usize, row: &mut [u8]| {
        let y_f = y as f32;
        let mut nx = b * y_f + c_mat;
        let mut ny = e * y_f + f_mat;
        let mut nw = h_mat * y_f + 1.0;
        let (dx, dy, dw) = (a, d, g);

        for x in 0..tw {
            let inv_w = 1.0 / nw;
            let sx = nx * inv_w;
            let sy = ny * inv_w;

            if sx >= 0.0 && sy >= 0.0 && sx < (w - 1) as f32 && sy < (h - 1) as f32 {
                let sx_int = sx as u32;
                let sy_int = sy as u32;
                let sx_frac = sx - sx_int as f32;
                let sy_frac = sy - sy_int as f32;

                let idx00 = ((sy_int * w + sx_int) * 4) as usize;
                let idx10 = idx00 + 4;
                let idx01 = (((sy_int + 1) * w + sx_int) * 4) as usize;
                let idx11 = idx01 + 4;

                let w00 = f32x4::from((1.0 - sx_frac) * (1.0 - sy_frac));
                let w10 = f32x4::from(sx_frac * (1.0 - sy_frac));
                let w01 = f32x4::from((1.0 - sx_frac) * sy_frac);
                let w11 = f32x4::from(sx_frac * sy_frac);

                unsafe {
                    let p00 = f32x4::from([
                        *input.get_unchecked(idx00) as f32,
                        *input.get_unchecked(idx00 + 1) as f32,
                        *input.get_unchecked(idx00 + 2) as f32,
                        *input.get_unchecked(idx00 + 3) as f32,
                    ]);
                    let p10 = f32x4::from([
                        *input.get_unchecked(idx10) as f32,
                        *input.get_unchecked(idx10 + 1) as f32,
                        *input.get_unchecked(idx10 + 2) as f32,
                        *input.get_unchecked(idx10 + 3) as f32,
                    ]);
                    let p01 = f32x4::from([
                        *input.get_unchecked(idx01) as f32,
                        *input.get_unchecked(idx01 + 1) as f32,
                        *input.get_unchecked(idx01 + 2) as f32,
                        *input.get_unchecked(idx01 + 3) as f32,
                    ]);
                    let p11 = f32x4::from([
                        *input.get_unchecked(idx11) as f32,
                        *input.get_unchecked(idx11 + 1) as f32,
                        *input.get_unchecked(idx11 + 2) as f32,
                        *input.get_unchecked(idx11 + 3) as f32,
                    ]);

                    let res_lin = srgb_to_linear(p00) * w00
                        + srgb_to_linear(p10) * w10
                        + srgb_to_linear(p01) * w01
                        + srgb_to_linear(p11) * w11;

                    let res_srgb: [f32; 4] = linear_to_srgb(res_lin).into();
                    let out_idx = x * 4;
                    *row.get_unchecked_mut(out_idx as usize) = res_srgb[0] as u8;
                    *row.get_unchecked_mut(out_idx as usize + 1) = res_srgb[1] as u8;
                    *row.get_unchecked_mut(out_idx as usize + 2) = res_srgb[2] as u8;
                    *row.get_unchecked_mut(out_idx as usize + 3) = res_srgb[3] as u8;
                }
            }
            nx += dx;
            ny += dy;
            nw += dw;
        }
    };

    #[cfg(feature = "rayon")]
    out.par_chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(y, row)| process_row(y, row));
    #[cfg(not(feature = "rayon"))]
    out.chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(y, row)| process_row(y, row));

    Some(out)
}
