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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lortunate.syringacropper.LocalNavBackStack
import com.lortunate.syringacropper.formatDebugSummary
import com.lortunate.syringacropper.perspective.PerspectiveCropState
import com.lortunate.syringacropper.perspective.PerspectiveCropper
import com.lortunate.syringacropper.perspective.PerspectiveSourceSize
import com.lortunate.syringacropper.perspective.rememberPerspectiveCropState
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerspectiveCropScreen() {
    val navBackStack = LocalNavBackStack.current
    val viewModel = viewModel { PerspectiveCropViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val previewBitmap = state.previewBitmap
    val cropState = key(previewBitmap) { rememberPerspectiveCropState() }

    val imagePickerLauncher = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        viewModel.onImagePicked(file)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Perspective Crop") },
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
            ActionPanel(
                onPickClick = { imagePickerLauncher.launch() },
                onClearClick = viewModel::clearImageSelection,
                onResetClick = {
                    cropState.resetSelection()
                    viewModel.updateInspectionResult(null)
                },
                onInspectClick = {
                    val sourceSize = state.sourceSize
                        ?: previewBitmap?.let { PerspectiveSourceSize(it.width, it.height) }
                        ?: return@ActionPanel
                    val inspectionResult = cropState.selectionOrNull(sourceSize)
                        ?.formatDebugSummary()
                        ?: "No active selection."
                    viewModel.updateInspectionResult(inspectionResult)
                },
                canClear = state.canClear,
                hasCropSelection = cropState.hasSelection
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

            CropperArea(
                modifier = Modifier.weight(1f),
                imageBitmap = previewBitmap,
                cropState = cropState
            )

            state.inspectionResult?.let { result ->
                InspectionResultCard(result = result)
            }
        }
    }
}

@Composable
private fun ActionPanel(
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
    onResetClick: () -> Unit,
    onInspectClick: () -> Unit,
    canClear: Boolean,
    hasCropSelection: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPickClick,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("Pick")
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
    }
}

@Composable
private fun CropperArea(
    modifier: Modifier = Modifier,
    imageBitmap: ImageBitmap?,
    cropState: PerspectiveCropState
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        if (imageBitmap != null) {
            PerspectiveCropper(
                imageBitmap = imageBitmap,
                state = cropState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun InspectionResultCard(result: String) {
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
