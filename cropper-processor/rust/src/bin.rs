use image::{ImageBuffer, Rgba};
use rayon::prelude::*;
use std::time::Instant;

fn perform_circle_original(
    input: &[u8],
    src_w: u32,
    src_h: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<Vec<u8>> {
    let mut img: ImageBuffer<Rgba<u8>, Vec<u8>> =
        ImageBuffer::from_raw(src_w, src_h, input.to_vec())?;
    let (cx, cy) = (x as f32 + w as f32 / 2.0, y as f32 + h as f32 / 2.0);
    let (rx, ry) = (w as f32 / 2.0, h as f32 / 2.0);
    for (px, py, pixel) in img.enumerate_pixels_mut() {
        let (dx, dy) = (px as f32 - cx, py as f32 - cy);
        if (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) > 1.0 {
            *pixel = Rgba([0, 0, 0, 0]);
        }
    }
    let mut res = Vec::with_capacity((w * h * 4) as usize);
    let input_raw = img.into_raw();
    for r in y..(y + h) {
        let s = ((r * src_w + x) * 4) as usize;
        res.extend_from_slice(&input_raw[s..(s + (w * 4) as usize)]);
    }
    Some(res)
}

fn perform_circle_optimized(
    input: &[u8],
    src_w: u32,
    _src_h: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<Vec<u8>> {
    let mut out = vec![0u8; (w * h * 4) as usize];
    let cx = w as f32 / 2.0;
    let cy = h as f32 / 2.0;
    let rx = w as f32 / 2.0;
    let ry = h as f32 / 2.0;

    let rx_sq = rx * rx;
    let ry_sq = ry * ry;

    out.par_chunks_exact_mut((w * 4) as usize)
        .enumerate()
        .for_each(|(r, row)| {
            let dy = r as f32 - cy + 0.5; // pixel center
            let dy_sq = dy * dy;

            let src_y = y + r as u32;
            let src_row_start = ((src_y * src_w + x) * 4) as usize;
            let src_slice = &input[src_row_start..(src_row_start + (w * 4) as usize)];

            for c in 0..w {
                let dx = c as f32 - cx + 0.5; // pixel center
                let dx_sq = dx * dx;

                if (dx_sq / rx_sq) + (dy_sq / ry_sq) <= 1.0 {
                    let idx = (c * 4) as usize;
                    row[idx..idx + 4].copy_from_slice(&src_slice[idx..idx + 4]);
                }
            }
        });

    Some(out)
}

fn main() {
    let sw = 4000;
    let sh = 3000;
    let input = vec![255u8; (sw * sh * 4) as usize];

    let x = 1000;
    let y = 500;
    let w = 2000;
    let h = 2000;

    let start = Instant::now();
    let _out = perform_circle_original(&input, sw, sh, x, y, w, h);
    println!("Circle original time: {:?}", start.elapsed());

    let start = Instant::now();
    let _out = perform_circle_optimized(&input, sw, sh, x, y, w, h);
    println!("Circle optimized time: {:?}", start.elapsed());
}
