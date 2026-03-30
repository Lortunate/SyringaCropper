package com.lortunate.syringacropper.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lortunate.syringacropper.CropResultSessionStore
import com.lortunate.syringacropper.LocalNavBackStack
import com.lortunate.syringacropper.RectCropResultRequest
import com.lortunate.syringacropper.formatDebugSummary
import com.lortunate.syringacropper.rect.RectCropAspectRatio
import com.lortunate.syringacropper.rect.RectCropState
import com.lortunate.syringacropper.rect.RectCropper
import com.lortunate.syringacropper.rect.rememberRectCropState
import com.lortunate.syringacropper.showCropResult
import com.lortunate.syringacropper.toCropSourceSize
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RectCropScreen() {
    val navBackStack = LocalNavBackStack.current
    val viewModel = viewModel { RectCropViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val previewBitmap = state.previewBitmap
    val cropState = key(previewBitmap) { rememberRectCropState() }

    // Synchronize aspect ratio
    cropState.setCropAspectRatio(state.aspectRatio)

    val imagePickerLauncher = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        viewModel.onImagePicked(file)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rect Crop") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RectActionPanel(
                aspectRatio = state.aspectRatio,
                onAspectRatioChange = viewModel::updateAspectRatio,
                onPickClick = { imagePickerLauncher.launch() },
                onClearClick = viewModel::clearImageSelection,
                onResetClick = {
                    cropState.resetSelection()
                    viewModel.updateInspectionResult(null)
                },
                onInspectClick = {
                    val sourceSize = state.sourceSize
                        ?: previewBitmap?.toCropSourceSize()
                        ?: return@RectActionPanel
                    val inspectionResult = cropState.selectionOrNull(sourceSize)
                        ?.formatDebugSummary() ?: "No active selection."
                    viewModel.updateInspectionResult(inspectionResult)
                },
                onCropClick = {
                    val bitmap = previewBitmap ?: return@RectActionPanel
                    val sourceSize = state.sourceSize ?: bitmap.toCropSourceSize()
                    val selection = cropState.selectionOrNull(sourceSize) ?: return@RectActionPanel
                    navBackStack.showCropResult(
                        RectCropResultRequest(
                            sourceBitmap = bitmap,
                            selection = selection,
                        ),
                    )
                },
                canClear = state.canClear,
                hasCropSelection = cropState.hasSelection,
            )

            if (state.isImageLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.imageLoadError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            RectCropperArea(
                modifier = Modifier.weight(1f),
                imageBitmap = previewBitmap,
                cropState = cropState,
            )

            state.inspectionResult?.let { result ->
                RectInspectionResultCard(result = result)
            }
        }
    }
}

@Composable
private fun RectActionPanel(
    aspectRatio: RectCropAspectRatio,
    onAspectRatioChange: (RectCropAspectRatio) -> Unit,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
    onResetClick: () -> Unit,
    onInspectClick: () -> Unit,
    onCropClick: () -> Unit,
    canClear: Boolean,
    hasCropSelection: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onPickClick,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Pick")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = onCropClick,
                enabled = hasCropSelection,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Crop")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onClearClick,
                modifier = Modifier.weight(1f),
                enabled = canClear,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Clear")
            }

            Button(
                onClick = onResetClick,
                modifier = Modifier.weight(1f),
                enabled = hasCropSelection,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Reset")
            }

            Button(
                onClick = onInspectClick,
                modifier = Modifier.weight(1f),
                enabled = hasCropSelection,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Inspect")
            }
        }

        val aspectRatios = listOf(
            "Free" to RectCropAspectRatio.Unconstrained,
            "1:1" to RectCropAspectRatio.Fixed.Square,
            "4:3" to RectCropAspectRatio.Fixed.Ratio4_3,
            "16:9" to RectCropAspectRatio.Fixed.Ratio16_9,
            "9:16" to RectCropAspectRatio.Fixed.Ratio9_16,
        )

        SelectionSegmentRow(
            title = "Aspect Ratio",
            options = aspectRatios,
            selected = aspectRatios.find { it.second == aspectRatio } ?: aspectRatios.first(),
            label = { it.first },
            onSelected = { onAspectRatioChange(it.second) },
        )
    }
}

@Composable
private fun <T> SelectionSegmentRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(label(option))
                }
            }
        }
    }
}

@Composable
private fun RectCropperArea(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap?,
    cropState: RectCropState,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        if (imageBitmap != null) {
            RectCropper(
                imageBitmap = imageBitmap,
                state = cropState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun RectInspectionResultCard(result: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        SelectionContainer {
            Text(
                text = result,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
