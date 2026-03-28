package com.lortunate.syringacropper.screen

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lortunate.syringacropper.perspective.PerspectiveSourceSize
import com.lortunate.syringacropper.readPreviewImageOrNull
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PerspectiveCropScreenState(
    val previewBitmap: ImageBitmap? = null,
    val sourceSize: PerspectiveSourceSize? = null,
    val isImageLoading: Boolean = false,
    val imageLoadError: String? = null,
    val inspectionResult: String? = null,
) {
    val canClear: Boolean
        get() = !isImageLoading && (previewBitmap != null || imageLoadError != null || inspectionResult != null)
}

class PerspectiveCropViewModel : ViewModel() {

    private val _state = MutableStateFlow(PerspectiveCropScreenState())
    val state = _state.asStateFlow()

    fun onImagePicked(file: PlatformFile?) {
        if (file == null) return

        _state.update {
            it.copy(
                isImageLoading = true,
                imageLoadError = null,
                inspectionResult = null,
            )
        }

        viewModelScope.launch {
            val imagePayload = withContext(Dispatchers.Default) {
                file.readPreviewImageOrNull()
            }

            _state.update { current ->
                current.copy(
                    previewBitmap = imagePayload?.previewBitmap,
                    sourceSize = imagePayload?.sourceSize,
                    isImageLoading = false,
                    imageLoadError = if (imagePayload == null) "Failed to decode selected image." else null,
                )
            }
        }
    }

    fun clearImageSelection() {
        _state.value = PerspectiveCropScreenState()
    }

    fun updateInspectionResult(result: String?) {
        _state.update { it.copy(inspectionResult = result) }
    }
}
