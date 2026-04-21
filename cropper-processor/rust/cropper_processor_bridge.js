import init, {circle_crop, perspective_warp, rect_crop, resize, rounded_rect_crop,} from "./cropper_processor.js";

await init();

const RECT_CROP_OP = 1;
const CIRCLE_CROP_OP = 2;
const ROUNDED_RECT_CROP_OP = 3;
const PERSPECTIVE_WARP_OP = 4;
const RESIZE_OP = 5;

const toUint8Array = (input) => Uint8Array.from(input, (value) => Number(value));
const toIntArray = (input) => Array.from(input, (value) => Number(value));
const toNumberArray = (input) => Array.from(input, (value) => Number(value));
const toFloat32Array = (input) => Float32Array.from(input, (value) => Number(value));
const toPlainArray = (output) => (output ? Array.from(output, (value) => Number(value)) : undefined);

export function process(input, sw, sh, types, params) {
    const operationTypes = toIntArray(types);
    const operationParams = toNumberArray(params);
    const operationType = operationTypes[0];
    const pixels = toUint8Array(input);

    switch (operationType) {
        case PERSPECTIVE_WARP_OP:
            return toPlainArray(
                perspective_warp(
                    pixels,
                    sw,
                    sh,
                    toFloat32Array(operationParams.slice(0, 8)),
                    operationParams[8],
                    operationParams[9],
                ),
            );
        case RECT_CROP_OP:
            return toPlainArray(
                rect_crop(
                    pixels,
                    sw,
                    sh,
                    operationParams[0],
                    operationParams[1],
                    operationParams[2],
                    operationParams[3],
                ),
            );
        case CIRCLE_CROP_OP:
            return toPlainArray(
                circle_crop(
                    pixels,
                    sw,
                    sh,
                    operationParams[0],
                    operationParams[1],
                    operationParams[2],
                    operationParams[3],
                ),
            );
        case ROUNDED_RECT_CROP_OP:
            return toPlainArray(
                rounded_rect_crop(
                    pixels,
                    sw,
                    sh,
                    operationParams[0],
                    operationParams[1],
                    operationParams[2],
                    operationParams[3],
                    operationParams[4],
                ),
            );
        case RESIZE_OP:
            return toPlainArray(resize(pixels, sw, sh, operationParams[0], operationParams[1]));
        default:
            throw new Error(`Unsupported cropper operation type: ${operationType}`);
    }
}
