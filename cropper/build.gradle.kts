import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

kotlin {
    android {
        namespace = "com.lortunate.syringacropper"
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
            baseName = "SyringaCropperKit"
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
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
    }

}

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
            name.set("SyringaCropper")
            description.set("Compose Multiplatform cropper UI library.")
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
