package com.lortunate.syringacropper.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lortunate.syringacropper.LocalNavBackStack
import com.lortunate.syringacropper.NavRoute


enum class Examples(val title: String, val route: NavRoute) {
    PERSPECTIVE_CROP("Perspective Crop", NavRoute.PerspectiveCrop),
    AVATAR_CROP("Avatar Crop", NavRoute.AvatarCrop),
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navBackStack = LocalNavBackStack.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "SyringaCropper") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = Examples.entries, key = { it.name }) {
                Card(
                    onClick = { navBackStack.add(it.route) }
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        text = it.title
                    )
                }
            }
        }
    }
}
