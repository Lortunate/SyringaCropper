import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

val wasmPackageKotlinDir = rootProject.layout.buildDirectory.dir("wasm/packages/${rootProject.name}-${project.name}/kotlin")
val cropperProcessorWasmResourcesDir = project(":cropper-processor").layout.buildDirectory.dir("processedResources/wasmJs/main")

kotlin {
    android {
        namespace = "com.lortunate.syringacropper.example"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt())
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":cropper"))
                implementation(project(":cropper-processor"))

                implementation(libs.material.icons.extended)
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val webMain by creating {
            dependsOn(commonMain)
        }
        val jsMain by getting {
            dependsOn(webMain)
        }
        val wasmJsMain by getting {
            dependsOn(webMain)
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
            }
        }
    }
}

val syncCropperProcessorWasmResources = tasks.register<Copy>("syncCropperProcessorWasmResources") {
    dependsOn(":cropper-processor:wasmJsProcessResources")
    dependsOn("wasmJsPublicPackageJson")
    from(cropperProcessorWasmResourcesDir)
    into(wasmPackageKotlinDir)
    include("cropper_processor.js", "cropper_processor_bridge.js", "cropper_processor_bg.wasm")
}

syncCropperProcessorWasmResources.configure {
    mustRunAfter("wasmJsProductionExecutableCompileSync")
    mustRunAfter("wasmJsDevelopmentExecutableCompileSync")
}

tasks.named("wasmJsBrowserProductionWebpack") {
    dependsOn(syncCropperProcessorWasmResources)
}

tasks.named("wasmJsBrowserDevelopmentWebpack") {
    dependsOn(syncCropperProcessorWasmResources)
}

tasks.named("wasmJsBrowserDevelopmentRun") {
    dependsOn(syncCropperProcessorWasmResources)
}

tasks.named("wasmJsProductionExecutableCompileSync") {
    finalizedBy(syncCropperProcessorWasmResources)
}

tasks.named("wasmJsDevelopmentExecutableCompileSync") {
    finalizedBy(syncCropperProcessorWasmResources)
}

compose.desktop {
    application {
        mainClass = "com.lortunate.syringacropper.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.lortunate.syringacropper"
            packageVersion = "1.0.0"
        }
    }
}
