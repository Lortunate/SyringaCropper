import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec

val rustAndroidTargets = providers.gradleProperty("rustAndroidTargets")
    .map { value ->
        value.split(",").map(String::trim).filter(String::isNotEmpty)
    }
    .orElse(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
    .get()
val rustDir = layout.projectDirectory.dir("rust")
val rustSourceDir = rustDir.dir("src")
val rustCargoToml = rustDir.file("Cargo.toml")
val rustCargoLock = rustDir.file("Cargo.lock")
val rustAndroidJniLibsDir = layout.projectDirectory.dir("src/androidMain/jniLibs")
val rustJvmOutputDir = layout.buildDirectory.dir("rust-jvm")
val rustWasmOutputDir = layout.buildDirectory.dir("rust-wasm/pkg")
val rustWasmBridgeFileName = "cropper_processor_bridge.js"
val rustWasmBridgeSourceFile = rustDir.file(rustWasmBridgeFileName)
val rustWasmGlueFile = rustWasmOutputDir.map { it.file("cropper_processor.js") }
val rustTaskGroup = "rust"
val legacyRustWasmInlineBridgeMarker = """
await __wbg_init();
const toUint8Array = (input) => Uint8Array.from(input, (value) => Number(value));
""".trimIndent()
val rustJvmLibraryName = when {
    System.getProperty("os.name").lowercase().contains("win") -> "cropper_processor.dll"
    System.getProperty("os.name").lowercase().contains("mac") -> "libcropper_processor.dylib"
    else -> "libcropper_processor.so"
}
val rustAndroidBuildCommand = buildList {
    add("cargo")
    add("ndk")
    add("--platform")
    add("24")
    rustAndroidTargets.forEach { target ->
        add("--target")
        add(target)
    }
    add("--output-dir")
    add(rustAndroidJniLibsDir.asFile.absolutePath)
    add("build")
    add("--release")
    add("--features")
    add("jni")
}
val rustWasmBuildCommand = listOf(
    "wasm-pack",
    "build",
    "rust",
    "--target",
    "web",
    "--release",
    "--no-pack",
    "--out-dir",
    rustWasmOutputDir.get().asFile.absolutePath,
    "--no-default-features",
    "--features",
    "wasm",
)
val rustGeneratedDirs = listOf(
    rustAndroidJniLibsDir,
    rustJvmOutputDir,
    rustWasmOutputDir,
)
val rustResourceTaskDependencies = listOf(
    "jvmProcessResources" to "buildRustJvm",
    "wasmJsProcessResources" to "buildRustWasm",
)
val rustAndroidTaskDependencies = setOf(
    "preAndroidMainBuild",
    "copyAndroidMainJniLibsProjectOnly",
    "mergeAndroidMainJniLibFolders",
    "mergeAndroidMainNativeLibs",
    "bundleAndroidMainAar",
)

fun Task.configureRustCargoInputs() {
    inputs.dir(rustSourceDir)
    listOf(rustCargoToml.asFile, rustCargoLock.asFile)
        .filter { it.exists() }
        .forEach(inputs::file)
}

fun Exec.cleanOutputDir(outputDir: java.io.File) {
    doFirst {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
    }
}

val buildRustJvmCargo = tasks.register<Exec>("buildRustJvmCargo") {
    group = rustTaskGroup
    configureRustCargoInputs()
    outputs.dir(rustJvmOutputDir)

    workingDir = rustDir.asFile
    commandLine = listOf("cargo", "build", "--release")
}

val buildRustJvm = tasks.register<Copy>("buildRustJvm") {
    group = rustTaskGroup
    dependsOn(buildRustJvmCargo)
    from(rustDir.file("target/release/$rustJvmLibraryName"))
    into(rustJvmOutputDir)
}

val buildRustAndroid = tasks.register<Exec>("buildRustAndroid") {
    group = rustTaskGroup
    configureRustCargoInputs()
    outputs.dir(rustAndroidJniLibsDir)

    workingDir = rustDir.asFile
    cleanOutputDir(rustAndroidJniLibsDir.asFile)
    commandLine(rustAndroidBuildCommand)
}

val buildRustWasmCargo = tasks.register<Exec>("buildRustWasmCargo") {
    group = rustTaskGroup
    configureRustCargoInputs()
    outputs.dir(rustWasmOutputDir)

    workingDir = projectDir
    cleanOutputDir(rustWasmOutputDir.get().asFile)
    commandLine(rustWasmBuildCommand)
}

val sanitizeRustWasmGlue = tasks.register("sanitizeRustWasmGlue") {
    dependsOn(buildRustWasmCargo)
    inputs.file(rustWasmGlueFile)
    inputs.property("marker", legacyRustWasmInlineBridgeMarker)
    outputs.file(rustWasmGlueFile)

    doLast {
        val file = rustWasmGlueFile.get().asFile
        if (!file.exists()) return@doLast

        val content = file.readText()
        val markerIndex = content.indexOf(legacyRustWasmInlineBridgeMarker)
        if (markerIndex >= 0) {
            file.writeText(content.substring(0, markerIndex).trimEnd() + System.lineSeparator())
        }
    }
}

val writeRustWasmBridge = tasks.register<Copy>("writeRustWasmBridge") {
    dependsOn(sanitizeRustWasmGlue)
    from(rustWasmBridgeSourceFile)
    into(rustWasmOutputDir)
}

val buildRustWasm = tasks.register("buildRustWasm") {
    group = rustTaskGroup
    dependsOn(writeRustWasmBridge)
}

rustResourceTaskDependencies.forEach { (taskName, dependencyName) ->
    tasks.named(taskName) {
        dependsOn(tasks.named(dependencyName))
    }
}

tasks.configureEach {
    if (name in rustAndroidTaskDependencies) {
        dependsOn(buildRustAndroid)
    }
}

tasks.named<Delete>("clean") {
    delete(rustGeneratedDirs)
}
