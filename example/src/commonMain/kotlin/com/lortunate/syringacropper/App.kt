package com.lortunate.syringacropper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lortunate.syringacropper.screen.HomeScreen
import com.lortunate.syringacropper.screen.PerspectiveCropScreen
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface NavRoute : NavKey {

    @Serializable
    data object Home : NavRoute

    @Serializable
    data object PerspectiveCrop : NavRoute

}


@OptIn(ExperimentalSerializationApi::class)
private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<NavRoute>()
        }
    }
}

val LocalNavBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("NavStack not provided")
}

@Composable
fun App() {
    val navBackStack = rememberNavBackStack(config, NavRoute.Home)
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(LocalNavBackStack provides navBackStack) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface {
                NavDisplay(navBackStack) { key ->
                    when (key) {
                        is NavRoute.Home -> NavEntry(key) {
                            HomeScreen()
                        }

                        is NavRoute.PerspectiveCrop -> NavEntry(key) {
                            PerspectiveCropScreen()
                        }

                        else -> throw IllegalArgumentException("Unknown route: $key")
                    }
                }
            }
        }
    }
}