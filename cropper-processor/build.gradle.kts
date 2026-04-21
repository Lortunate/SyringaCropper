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
    js {
        browser()
    }
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
        val commonMain by getting {
            dependencies {
                implementation(project(":cropper"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val unsupportedMain by creating {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        val jsMain by getting {
            dependsOn(unsupportedMain)
        }
        val iosX64Main by getting {
            dependsOn(unsupportedMain)
        }
        val iosArm64Main by getting {
            dependsOn(unsupportedMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(unsupportedMain)
        }
        val jvmMain by getting {
            resources.srcDirs(rustJvmOutputDir)
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        val wasmJsMain by getting {
            resources.srcDirs(rustWasmOutputDir)
        }
        val skiaMain by creating {
            dependsOn(commonMain)
        }

        jvmMain.dependsOn(skiaMain)
        wasmJsMain.dependsOn(skiaMain)
    }
}

apply(from = "rust.gradle.kts")
