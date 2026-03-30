package com.lortunate.syringacropper.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lortunate.syringacropper.CropResultRequest
import com.lortunate.syringacropper.CropResultSessionStore
import com.lortunate.syringacropper.ExampleCropExecutor
import com.lortunate.syringacropper.LocalNavBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CropResultScreenState(
    val isEmpty: Boolean = false,
    val resultBitmap: ImageBitmap? = null,
    val isCropping: Boolean = false,
    val errorMessage: String? = null,
)

private class CropResultViewModel : ViewModel() {
    private val _state = MutableStateFlow(CropResultScreenState())
    val state = _state.asStateFlow()
    private var latestHandledRequestId: Long? = null

    init {
        val supportMessage = ExampleCropExecutor.supportMessage
        if (supportMessage != null) {
            _state.value = CropResultScreenState(errorMessage = supportMessage)
        } else {
            viewModelScope.launch {
                CropResultSessionStore.latestRequestId.collect { requestId ->
                    if (requestId == null || requestId == latestHandledRequestId) {
                        if (latestHandledRequestId == null) {
                            _state.value = CropResultScreenState(isEmpty = true)
                        }
                        return@collect
                    }

                    val request = CropResultSessionStore.consume(requestId)
                    if (request == null) {
                        if (latestHandledRequestId == null) {
                            _state.value = CropResultScreenState(isEmpty = true)
                        }
                        return@collect
                    }

                    latestHandledRequestId = requestId
                    crop(request)
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropResultScreen() {
    val navBackStack = LocalNavBackStack.current
    val viewModel = viewModel { CropResultViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val resultBitmap = state.resultBitmap

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crop Result") },
                navigationIcon = {
                    IconButton(onClick = { navBackStack.removeLast() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            when {
                state.isEmpty -> {
                    Text(
                        text = "No pending crop request.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                state.isCropping -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                resultBitmap != null -> {
                    ResultImageCard(
                        bitmap = resultBitmap,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    Text(
                        text = "Crop result is unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultImageCard(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Crop result",
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
