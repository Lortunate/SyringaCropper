use crate::core;
use wasm_bindgen::prelude::*;

fn corners_to_array(corners: &[f32]) -> Option<[f32; 8]> {
    if corners.len() != 8 {
        return None;
    }
    Some([
        corners[0], corners[1], corners[2], corners[3], corners[4], corners[5], corners[6],
        corners[7],
    ])
}

#[wasm_bindgen]
pub fn perspective_warp(
    input: &[u8],
    w: u32,
    h: u32,
    corners: &[f32],
    tw: u32,
    th: u32,
) -> Option<Vec<u8>> {
    core::perform_warp(input, w, h, &corners_to_array(corners)?, tw, th)
}

#[wasm_bindgen]
pub fn rect_crop(
    input: &[u8],
    sw: u32,
    sh: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<Vec<u8>> {
    core::perform_rect(input, sw, sh, x, y, w, h)
}

#[wasm_bindgen]
pub fn circle_crop(
    input: &[u8],
    sw: u32,
    sh: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
) -> Option<Vec<u8>> {
    core::perform_circle(input, sw, sh, x, y, w, h)
}

#[wasm_bindgen]
pub fn rounded_rect_crop(
    input: &[u8],
    sw: u32,
    sh: u32,
    x: u32,
    y: u32,
    w: u32,
    h: u32,
    r: f32,
) -> Option<Vec<u8>> {
    core::perform_rounded_rect(input, sw, sh, x, y, w, h, r)
}

#[wasm_bindgen]
pub fn resize(input: &[u8], sw: u32, sh: u32, dw: u32, dh: u32) -> Option<Vec<u8>> {
    core::perform_resize(input, sw, sh, dw, dh)
}
