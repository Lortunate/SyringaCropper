use wide::f32x4;

#[inline(always)]
pub fn srgb_to_linear(v: f32x4) -> f32x4 {
    let inv_255 = f32x4::from(1.0 / 255.0);
    let v_norm = v * inv_255;
    let mut arr = (v_norm * v_norm).to_array();
    arr[3] = v_norm.to_array()[3];
    f32x4::from(arr)
}

#[inline(always)]
pub fn linear_to_srgb(v: f32x4) -> f32x4 {
    let mut arr = v.sqrt().to_array();
    arr[3] = v.to_array()[3];
    f32x4::from(arr) * f32x4::from(255.0)
}
