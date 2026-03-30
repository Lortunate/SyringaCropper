use crate::core::{Operation, ProcessorPipeline};
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JByteBuffer, JClass, JFloatArray, JIntArray};
use jni::strings::JNIString;
use jni::sys::{jbyteArray, jint};
use jni::{Env, EnvUnowned};
use jni_fn::jni_fn;
use std::ptr;

const RECT_PARAM_COUNT: usize = 4;
const ROUNDED_RECT_PARAM_COUNT: usize = 5;
const WARP_PARAM_COUNT: usize = 10;
const RESIZE_PARAM_COUNT: usize = 2;

fn throw_illegal_argument<S: AsRef<str>>(env: &mut Env, message: S) -> jbyteArray {
    if let Ok(class) = env.find_class(JNIString::from("java/lang/IllegalArgumentException")) {
        let _ = env.throw_new(class, JNIString::from(message.as_ref()));
    }
    ptr::null_mut()
}

fn read_int_array(env: &Env, values: &JIntArray) -> Result<Vec<i32>, &'static str> {
    let len = values
        .len(env)
        .map_err(|_| "process could not read operation types.")?;
    let mut output = vec![0i32; len as usize];
    values
        .get_region(env, 0, &mut output)
        .map_err(|_| "process could not read operation types.")?;
    Ok(output)
}

fn read_float_array(env: &Env, values: &JFloatArray) -> Result<Vec<f32>, &'static str> {
    let len = values
        .len(env)
        .map_err(|_| "process could not read operation parameters.")?;
    let mut output = vec![0.0f32; len as usize];
    values
        .get_region(env, 0, &mut output)
        .map_err(|_| "process could not read operation parameters.")?;
    Ok(output)
}

fn required_params(op_type: i32) -> Option<usize> {
    match op_type {
        1 | 2 => Some(RECT_PARAM_COUNT),
        3 => Some(ROUNDED_RECT_PARAM_COUNT),
        4 => Some(WARP_PARAM_COUNT),
        5 => Some(RESIZE_PARAM_COUNT),
        _ => None,
    }
}

fn decode_operation(
    op_type: i32,
    params: &[f32],
    p_idx: &mut usize,
) -> Result<Option<Operation>, String> {
    let Some(required_params) = required_params(op_type) else {
        return Ok(None);
    };

    if *p_idx + required_params > params.len() {
        return Err(format!(
            "process expected {} params for operation type {}, but only {} remained.",
            required_params,
            op_type,
            params.len().saturating_sub(*p_idx),
        ));
    }

    let op = match op_type {
        1 => Operation::RectCrop {
            x: params[*p_idx] as u32,
            y: params[*p_idx + 1] as u32,
            w: params[*p_idx + 2] as u32,
            h: params[*p_idx + 3] as u32,
        },
        2 => Operation::CircleCrop {
            x: params[*p_idx] as u32,
            y: params[*p_idx + 1] as u32,
            w: params[*p_idx + 2] as u32,
            h: params[*p_idx + 3] as u32,
        },
        3 => Operation::RoundedRectCrop {
            x: params[*p_idx] as u32,
            y: params[*p_idx + 1] as u32,
            w: params[*p_idx + 2] as u32,
            h: params[*p_idx + 3] as u32,
            r: params[*p_idx + 4],
        },
        4 => Operation::Warp {
            corners: [
                params[*p_idx],
                params[*p_idx + 1],
                params[*p_idx + 2],
                params[*p_idx + 3],
                params[*p_idx + 4],
                params[*p_idx + 5],
                params[*p_idx + 6],
                params[*p_idx + 7],
            ],
            tw: params[*p_idx + 8] as u32,
            th: params[*p_idx + 9] as u32,
        },
        5 => Operation::Resize {
            tw: params[*p_idx] as u32,
            th: params[*p_idx + 1] as u32,
        },
        _ => unreachable!(),
    };

    *p_idx += required_params;
    Ok(Some(op))
}

fn process_impl(
    env: &mut Env,
    buffer: &JByteBuffer,
    w: jint,
    h: jint,
    ops_types: &JIntArray,
    ops_params: &JFloatArray,
) -> jbyteArray {
    if w <= 0 || h <= 0 {
        return throw_illegal_argument(
            env,
            format!(
                "process requires a positive source size, but was {}x{}.",
                w, h
            ),
        );
    }

    let addr = match env.get_direct_buffer_address(buffer) {
        Ok(addr) => addr,
        Err(_) => {
            return throw_illegal_argument(env, "process requires a direct ByteBuffer source.");
        }
    };
    let size = match env.get_direct_buffer_capacity(buffer) {
        Ok(size) if size > 0 => size,
        Ok(_) => return throw_illegal_argument(env, "process requires a non-empty source buffer."),
        Err(_) => {
            return throw_illegal_argument(
                env,
                "process could not read the source buffer capacity.",
            );
        }
    };
    let expected_size = match (w as usize)
        .checked_mul(h as usize)
        .and_then(|pixels| pixels.checked_mul(4))
    {
        Some(size) => size,
        None => {
            return throw_illegal_argument(
                env,
                "process source size overflowed the native buffer bounds.",
            );
        }
    };
    if size < expected_size {
        return throw_illegal_argument(
            env,
            format!(
                "process source buffer was {} bytes, expected at least {} bytes for {}x{} ARGB_8888 input.",
                size, expected_size, w, h
            ),
        );
    }

    let input = unsafe { std::slice::from_raw_parts(addr, size) };
    let mut pipeline = ProcessorPipeline::new(input, w as u32, h as u32);

    let types = match read_int_array(env, ops_types) {
        Ok(types) => types,
        Err(message) => return throw_illegal_argument(env, message),
    };
    let params = match read_float_array(env, ops_params) {
        Ok(params) => params,
        Err(message) => return throw_illegal_argument(env, message),
    };

    let mut p_idx = 0usize;
    for op_type in types {
        let op = match decode_operation(op_type, &params, &mut p_idx) {
            Ok(Some(op)) => op,
            Ok(None) => continue,
            Err(message) => return throw_illegal_argument(env, message),
        };

        let Some(next_pipeline) = pipeline.apply(op) else {
            return throw_illegal_argument(
                env,
                format!(
                    "process rejected invalid parameters for operation type {}.",
                    op_type
                ),
            );
        };
        pipeline = next_pipeline;
    }

    let (res_data, _, _) = pipeline.into_inner();
    env.byte_array_from_slice(&res_data)
        .ok()
        .map(|array| array.into_raw())
        .unwrap_or(ptr::null_mut())
}

#[jni_fn("com.lortunate.syringacropper.processor.CropperProcessor")]
pub fn process(
    mut unowned_env: EnvUnowned,
    _: JClass,
    buffer: JByteBuffer,
    w: jint,
    h: jint,
    ops_types: JIntArray,
    ops_params: JFloatArray,
) -> jbyteArray {
    unowned_env
        .with_env(|env| -> Result<jbyteArray, jni::errors::Error> {
            Ok(process_impl(env, &buffer, w, h, &ops_types, &ops_params))
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}
