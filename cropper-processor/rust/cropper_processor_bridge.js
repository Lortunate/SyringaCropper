import init, {circle_crop, perspective_warp, rect_crop, resize, rounded_rect_crop,} from "./cropper_processor.js";

await init();

const toUint8Array = (input) => Uint8Array.from(input, (value) => Number(value));
const toFloat32Array = (input) => Float32Array.from(input, (value) => Number(value));
const toPlainArray = (output) => (output ? Array.from(output, (value) => Number(value)) : undefined);

export function perspectiveWarp(input, w, h, corners, tw, th) {
    return toPlainArray(perspective_warp(toUint8Array(input), w, h, toFloat32Array(corners), tw, th));
}

export function rectCrop(input, sw, sh, x, y, w, h) {
    return toPlainArray(rect_crop(toUint8Array(input), sw, sh, x, y, w, h));
}

export function circleCrop(input, sw, sh, x, y, w, h) {
    return toPlainArray(circle_crop(toUint8Array(input), sw, sh, x, y, w, h));
}

export function roundedRectCrop(input, sw, sh, x, y, w, h, r) {
    return toPlainArray(rounded_rect_crop(toUint8Array(input), sw, sh, x, y, w, h, r));
}

export function resizeImage(input, sw, sh, dw, dh) {
    return toPlainArray(resize(toUint8Array(input), sw, sh, dw, dh));
}
