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
import com.lortunate.syringacropper.inspect
import com.lortunate.syringacropper.LocalNavBackStack
import com.lortunate.syringacropper.avatar.AvatarCropShape
import com.lortunate.syringacropper.avatar.AvatarCropState
import com.lortunate.syringacropper.avatar.AvatarCropper
import com.lortunate.syringacropper.avatar.rememberAvatarCropState
import com.lortunate.syringacropper.resolveSourceSize
import com.lortunate.syringacropper.showCropResult
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCropScreen() {
    val navBackStack = LocalNavBackStack.current
    val viewModel = viewModel { AvatarCropViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val previewBitmap = state.previewBitmap
    val cropState = key(previewBitmap) { rememberAvatarCropState() }
    val sourceSize = previewBitmap.resolveSourceSize(state.sourceSize)

    val imagePickerLauncher = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        viewModel.onImagePicked(file)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Avatar Crop") },
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
            AvatarActionPanel(
                shape = state.shape,
                onShapeChange = viewModel::updateShape,
                onPickClick = { imagePickerLauncher.launch() },
                onClearClick = viewModel::clearImageSelection,
                onResetClick = {
                    cropState.resetSelection()
                    viewModel.updateInspectionResult(null)
                },
                onInspectClick = {
                    viewModel.updateInspectionResult(cropState.inspect(sourceSize, state.shape))
                },
                onCropClick = {
                    val bitmap = previewBitmap ?: return@AvatarActionPanel
                    val selection = cropState.selectionOrNull(
                        shape = state.shape,
                        sourceSize = sourceSize ?: return@AvatarActionPanel,
                    ) ?: return@AvatarActionPanel
                    navBackStack.showCropResult(sourceBitmap = bitmap, selection = selection)
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

            AvatarCropperArea(
                modifier = Modifier.weight(1f),
                imageBitmap = previewBitmap,
                cropState = cropState,
                shape = state.shape,
            )

            state.inspectionResult?.let { result ->
                AvatarInspectionResultCard(result = result)
            }
        }
    }
}

@Composable
private fun AvatarActionPanel(
    shape: AvatarCropShape,
    onShapeChange: (AvatarCropShape) -> Unit,
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

        SelectionSegmentRow(
            title = "Shape",
            options = AvatarCropShape.entries,
            selected = shape,
            label = { option -> option.name.lowercase().replaceFirstChar(Char::titlecase) },
            onSelected = onShapeChange,
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
private fun AvatarCropperArea(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap?,
    cropState: AvatarCropState,
    shape: AvatarCropShape,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        if (imageBitmap != null) {
            AvatarCropper(
                imageBitmap = imageBitmap,
                state = cropState,
                shape = shape,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AvatarInspectionResultCard(result: String) {
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
