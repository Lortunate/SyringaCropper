package com.lortunate.syringacropper.screen

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lortunate.syringacropper.CropResultRequest
import com.lortunate.syringacropper.CropResultSessionStore
import com.lortunate.syringacropper.processor.CropperProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CropResultScreenState(
    val isEmpty: Boolean = false,
    val resultBitmap: ImageBitmap? = null,
    val isCropping: Boolean = false,
    val errorMessage: String? = null,
)

class CropResultViewModel(
    requestId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(CropResultScreenState())
    val state = _state.asStateFlow()

    init {
        val supportMessage = CropperProcessor.supportMessage
        if (supportMessage != null) {
            _state.value = CropResultScreenState(errorMessage = supportMessage)
        } else {
            val request = CropResultSessionStore.consume(requestId)
            if (request == null) {
                _state.value = CropResultScreenState(isEmpty = true)
            } else {
                crop(request)
            }
        }
    }

    private fun crop(request: CropResultRequest) {
        _state.update {
            it.copy(
                isEmpty = false,
                isCropping = true,
                resultBitmap = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                runCatching { request.crop() }
            }
            _state.update {
                it.copy(
                    isCropping = false,
                    resultBitmap = cropped.getOrNull(),
                    errorMessage = cropped.exceptionOrNull()?.message,
                )
            }
        }
    }
}
