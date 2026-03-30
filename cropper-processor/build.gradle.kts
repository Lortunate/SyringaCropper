import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
}

val rustJvmOutputDir: Provider<Directory> = layout.buildDirectory.dir("rust-jvm")
val rustWasmOutputDir: Provider<Directory> = layout.buildDirectory.dir("rust-wasm/pkg")

kotlin {
    jvm()
    android {
        namespace = "com.lortunate.syringacropper.processor"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
        minSdk = 24
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "CropperProcessorKit"
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":cropper"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        jvmMain {
            resources.srcDirs(rustJvmOutputDir)
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        wasmJsMain {
            resources.srcDirs(rustWasmOutputDir)
        }
    }
}

apply(from = "rust.gradle.kts")
