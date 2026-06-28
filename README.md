## SyringaCropper

Compose Multiplatform image cropper library with a sample app.

### Setup

Add the GitHub Packages Maven repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/lortunate/SyringaCropper")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .get()
            }
        }
    }
}
```

Set credentials in `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.key=your-github-token
```

Add the published packages:

```kotlin
commonMain.dependencies {
    implementation("com.lortunate:cropper:0.0.1")
    implementation("com.lortunate:cropper-processor:0.0.1")
}
```

### Usage

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.lortunate.syringacropper.CropSourceSize
import com.lortunate.syringacropper.rect.RectCropper
import com.lortunate.syringacropper.rect.rememberRectCropState

@Composable
fun CropperExample(imageBitmap: ImageBitmap) {
    val cropState = rememberRectCropState()

    RectCropper(
        imageBitmap = imageBitmap,
        state = cropState,
        modifier = Modifier.fillMaxSize(),
    )

    val normalizedSelection = cropState.normalizedRectOrNull()
    val sourceSelection = cropState.selectionOrNull(
        CropSourceSize(
            width = imageBitmap.width,
            height = imageBitmap.height,
        )
    )
}
```

`normalizedRectOrNull()` returns a normalized `Rect` in the `0..1` range.
`selectionOrNull(...)` converts the selection to the source image coordinates.
