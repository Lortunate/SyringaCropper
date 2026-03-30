use fast_image_resize as fr;
#[cfg(feature = "rayon")]
use rayon::prelude::*;

fn validate_crop_bounds(
    input: &[u8],
    src_w: u32,
    src_h: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<()> {
    if src_w == 0 || src_h == 0 || w == 0 || h == 0 {
        return None;
    }

    let expected_len = src_w.checked_mul(src_h)?.checked_mul(4)?.try_into().ok()?;
    if input.len() < expected_len {
        return None;
    }

    let right = x.checked_add(w)?;
    let bottom = y.checked_add(h)?;
    if right > src_w || bottom > src_h {
        return None;
    }

    Some(())
}

pub fn perform_rect(
    input: &[u8],
    src_w: u32,
    src_h: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<Vec<u8>> {
    validate_crop_bounds(input, src_w, src_h, x, y, w, h)?;
    let mut res = vec![0u8; (w * h * 4) as usize];
    let row_len = (w * 4) as usize;

    let process_row = |r: usize, row: &mut [u8]| {
        let src_y = y + r as u32;
        let s = ((src_y * src_w + x) * 4) as usize;
        row.copy_from_slice(&input[s..(s + row_len)]);
    };

    #[cfg(feature = "rayon")]
    res.par_chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(r, row)| process_row(r, row));
    #[cfg(not(feature = "rayon"))]
    res.chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(r, row)| process_row(r, row));

    Some(res)
}

pub fn perform_circle(
    input: &[u8],
    src_w: u32,
    src_h: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<Vec<u8>> {
    validate_crop_bounds(input, src_w, src_h, x, y, w, h)?;
    let mut out = vec![0u8; (w * h * 4) as usize];
    let row_len = (w * 4) as usize;
    let (cx, cy) = (w as f32 / 2.0, h as f32 / 2.0);
    let (rx, ry) = (w as f32 / 2.0, h as f32 / 2.0);

    let process_row = |r: usize, row: &mut [u8]| {
        let dy = r as f32 - cy + 0.5;
        let dy_norm_sq = (dy / ry).powi(2);
        let src_y = y + r as u32;
        let src_row_start = ((src_y * src_w + x) * 4) as usize;
        let src_slice = &input[src_row_start..(src_row_start + row_len)];

        for c in 0..w {
            let dx = c as f32 - cx + 0.5;
            let dist_sq = (dx / rx).powi(2) + dy_norm_sq;
            let idx = (c * 4) as usize;
            if dist_sq <= 1.0 {
                row[idx..idx + 3].copy_from_slice(&src_slice[idx..idx + 3]);
                let dist = dist_sq.sqrt();
                let alpha = if dist > 0.99 {
                    ((1.0 - dist) / 0.01).clamp(0.0, 1.0)
                } else {
                    1.0
                };
                row[idx + 3] = (src_slice[idx + 3] as f32 * alpha) as u8;
            } else {
                row[idx..idx + 4].fill(0);
            }
        }
    };

    #[cfg(feature = "rayon")]
    out.par_chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(r, row)| process_row(r, row));
    #[cfg(not(feature = "rayon"))]
    out.chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(r, row)| process_row(r, row));

    Some(out)
}

pub fn perform_rounded_rect(
    input: &[u8],
    src_w: u32,
    src_h: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
    r: f32,
) -> Option<Vec<u8>> {
    validate_crop_bounds(input, src_w, src_h, x, y, w, h)?;
    let mut out = vec![0u8; (w * h * 4) as usize];
    let row_len = (w * 4) as usize;

    let process_row = |py: usize, row: &mut [u8]| {
        let y_f = py as f32 + 0.5;
        let src_y = y + py as u32;
        let src_row_start = ((src_y * src_w + x) * 4) as usize;
        let src_slice = &input[src_row_start..(src_row_start + row_len)];

        for px in 0..w {
            let x_f = px as f32 + 0.5;
            let dx = (x_f - w as f32 / 2.0).abs() - (w as f32 / 2.0 - r);
            let dy = (y_f - h as f32 / 2.0).abs() - (h as f32 / 2.0 - r);
            let dist = if dx > 0.0 && dy > 0.0 {
                (dx * dx + dy * dy).sqrt() - r
            } else {
                dx.max(dy) - r
            };

            let idx = (px * 4) as usize;
            if dist <= 0.0 {
                row[idx..idx + 3].copy_from_slice(&src_slice[idx..idx + 3]);
                let alpha = if dist > -1.0 {
                    ((-dist) / 1.0).clamp(0.0, 1.0)
                } else {
                    1.0
                };
                row[idx + 3] = (src_slice[idx + 3] as f32 * alpha) as u8;
            } else {
                row[idx..idx + 4].fill(0);
            }
        }
    };

    #[cfg(feature = "rayon")]
    out.par_chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(py, row)| process_row(py, row));
    #[cfg(not(feature = "rayon"))]
    out.chunks_exact_mut(row_len)
        .enumerate()
        .for_each(|(py, row)| process_row(py, row));

    Some(out)
}

pub fn perform_resize(
    input: &[u8],
    src_w: u32,
    src_h: u32,
    dst_w: u32,
    dst_h: u32,
) -> Option<Vec<u8>> {
    let src = fr::images::ImageRef::new(src_w, src_h, input, fr::PixelType::U8x4).ok()?;
    let mut dst = fr::images::Image::new(dst_w, dst_h, fr::PixelType::U8x4);
    let mut resizer = fr::Resizer::new();
    let options = fr::ResizeOptions::default(); // Convolution(Lanczos3) is the default in v5
    resizer.resize(&src, &mut dst, &options).ok()?;
    Some(dst.into_vec())
}

#[cfg(test)]
mod tests {
    use super::{perform_circle, perform_rect, perform_rounded_rect};

    fn sample_image(width: u32, height: u32) -> Vec<u8> {
        vec![0x7f; (width * height * 4) as usize]
    }

    #[test]
    fn rect_crop_rejects_out_of_bounds_region() {
        let input = sample_image(4, 4);

        let result = perform_rect(&input, 4, 4, 3, 0, 2, 2);

        assert!(result.is_none());
    }

    #[test]
    fn circle_crop_rejects_short_input_buffer() {
        let result = perform_circle(&[0u8; 8], 4, 4, 0, 0, 2, 2);

        assert!(result.is_none());
    }

    #[test]
    fn rounded_rect_crop_rejects_zero_sized_region() {
        let input = sample_image(4, 4);

        let result = perform_rounded_rect(&input, 4, 4, 0, 0, 0, 2, 1.0);

        assert!(result.is_none());
    }
}
