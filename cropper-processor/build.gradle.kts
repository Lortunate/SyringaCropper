import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    id("maven-publish")
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lortunate/SyringaCropper")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("SyringaCropper Processor")
            description.set("Multiplatform image processing backend for SyringaCropper.")
            url.set("https://github.com/Lortunate/SyringaCropper")
            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://github.com/Lortunate/SyringaCropper")
                connection.set("scm:git:git://github.com/Lortunate/SyringaCropper.git")
                developerConnection.set("scm:git:ssh://git@github.com/Lortunate/SyringaCropper.git")
            }
        }
    }
}
