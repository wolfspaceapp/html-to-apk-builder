package com.htmltoapk.builder.android

import android.util.Log
import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.signing.ApkSigner
import com.htmltoapk.builder.signing.SigningKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

class ApkBuilder(private val workspace: File) {
    companion object {
        private const val TAG = "ApkBuilder"
    }

    private var aapt2Path: String? = null

    init {
        findAapt2()
    }

    suspend fun buildApk(
        outputFile: File,
        project: Project,
        signingKey: SigningKey? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Building APK: ${outputFile.absolutePath}")

                // Create unsigned APK using aapt2
                val unsignedApk = File(workspace, "unsigned.apk")
                val success = if (aapt2Path != null) {
                    buildApkWithAapt2(unsignedApk, project)
                } else {
                    Log.w(TAG, "aapt2 not found, using fallback ZIP-based build")
                    createApkFallback(unsignedApk, project)
                }

                if (!success) {
                    return@withContext false
                }

                // Sign if signing key provided
                if (signingKey != null) {
                    val signer = ApkSigner()
                    val signed = signer.signApk(unsignedApk, signingKey, outputFile)
                    if (!signed) {
                        return@withContext false
                    }
                } else {
                    // Just copy unsigned to output
                    unsignedApk.copyTo(outputFile, overwrite = true)
                }

                // Verify
                verifyApk(outputFile)

                Log.d(TAG, "APK built successfully: ${outputFile.absolutePath}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build APK", e)
                false
            }
        }
    }

    suspend fun buildAab(
        outputFile: File,
        project: Project,
        signingKey: SigningKey? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Building AAB: ${outputFile.absolutePath}")

                // AAB building requires bundletool
                val bundletoolPath = findBundletool()
                if (bundletoolPath != null) {
                    return@withContext buildAabWithBundletool(outputFile, project, signingKey, bundletoolPath)
                }

                Log.w(TAG, "bundletool not found, falling back to APK")
                buildApk(outputFile, project, signingKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build AAB", e)
                false
            }
        }
    }

    private fun findAapt2() {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val buildTools = File(androidHome, "build-tools")
            if (buildTools.exists()) {
                buildTools.listFiles()?.sortedByDescending { it.name }?.forEach { versionDir ->
                    val aapt2 = File(versionDir, if (isWindows()) "aapt2.exe" else "aapt2")
                    if (aapt2.exists()) {
                        aapt2Path = aapt2.absolutePath
                        Log.d(TAG, "Found aapt2 at: $aapt2Path")
                        return
                    }
                }
            }
        }
        Log.w(TAG, "aapt2 not found")
    }

    private fun findBundletool(): String? {
        // Check common locations for bundletool
        val possiblePaths = mutableListOf<String>()

        // Check in workspace
        val workspaceBundletool = File(workspace, "bundletool.jar")
        if (workspaceBundletool.exists()) {
            return workspaceBundletool.absolutePath
        }

        // Check in Android SDK
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            possiblePaths.add(File(androidHome, "bundletool/bundletool.jar").absolutePath)
        }

        // Check in user home
        possiblePaths.add(File(System.getProperty("user.home"), ".local/bin/bundletool.jar").absolutePath)
        possiblePaths.add(File(System.getProperty("user.home"), "bundletool.jar").absolutePath)

        for (path in possiblePaths) {
            if (File(path).exists()) {
                return path
            }
        }

        return null
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

    private fun buildApkWithAapt2(outputApk: File, project: Project): Boolean {
        val manifestFile = File(workspace, "AndroidManifest.xml")
        val resDir = File(workspace, "res")
        val assetsDir = File(workspace, "assets")
        val dexDir = workspace

        try {
            // Step 1: Compile resources with aapt2 compile
            val compiledResDir = File(workspace, "compiled_res")
            compiledResDir.mkdirs()

            if (resDir.exists()) {
                val resFiles = resDir.walkTopDown().filter { it.isFile }.toList()
                for (resFile in resFiles) {
                    val relativePath = resFile.path.substring(resDir.path.length + 1)
                    val outputFile = File(compiledResDir, relativePath + ".flat")
                    outputFile.parentFile.mkdirs()

                    val compileCmd = listOf(
                        aapt2Path!!, "compile",
                        "--dir", resDir.absolutePath,
                        "-o", compiledResDir.absolutePath
                    )

                    val process = ProcessBuilder(compileCmd).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = process.waitFor()

                    if (exitCode != 0) {
                        Log.e(TAG, "aapt2 compile failed: $output")
                        return false
                    }
                }
            }

            // Step 2: Link resources with aapt2 link
            val linkCmd = mutableListOf(
                aapt2Path!!, "link",
                "--manifest", manifestFile.absolutePath,
                "-o", outputApk.absolutePath,
                "--min-sdk-version", project.minSdk.toString(),
                "--target-sdk-version", project.targetSdk.toString()
            )

            project.maxSdk?.let { linkCmd.add("--max-sdk-version"); linkCmd.add(it.toString()) }

            // Add compiled resources
            if (compiledResDir.exists()) {
                val compiledResZip = File(workspace, "compiled_res.zip")
                zipDirectory(compiledResDir, compiledResZip)
                linkCmd.add("-I")
                linkCmd.add(getAndroidJar())
                linkCmd.add("--proto-format")
                linkCmd.add("--no-resource-deduping")
                linkCmd.add("--no-version-vectors")
                linkCmd.add(compiledResZip.absolutePath)
            }

            // Add assets
            if (assetsDir.exists()) {
                linkCmd.add("--assets")
                linkCmd.add(assetsDir.absolutePath)
            }

            // Add DEX files
            val dexFiles = dexDir.walkTopDown().filter { it.extension == "dex" }.sortedBy { it.name }
            for ((index, dexFile) in dexFiles.withIndex()) {
                linkCmd.add("--dex-file")
                linkCmd.add(dexFile.absolutePath)
            }

            // Execute link
            val linkProcess = ProcessBuilder(linkCmd).redirectErrorStream(true).start()
            val linkOutput = linkProcess.inputStream.bufferedReader().readText()
            val linkExitCode = linkProcess.waitFor()

            if (linkExitCode != 0) {
                Log.e(TAG, "aapt2 link failed: $linkOutput")
                return false
            }

            Log.d(TAG, "aapt2 link successful")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "aapt2 build failed", e)
            return false
        }
    }

    private fun getAndroidJar(): String {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val platforms = File(androidHome, "platforms")
            if (platforms.exists()) {
                val platformDirs = platforms.listFiles()?.filter { it.name.startsWith("android-") }?.sortedByDescending { it.name }
                platformDirs?.firstOrNull()?.let { platformDir ->
                    val androidJar = File(platformDir, "android.jar")
                    if (androidJar.exists()) {
                        return androidJar.absolutePath
                    }
                }
            }
        }
        // Fallback
        return "android.jar"
    }

    private fun buildAabWithBundletool(
        outputAab: File,
        project: Project,
        signingKey: SigningKey?,
        bundletoolPath: String
    ): Boolean {
        // For AAB, we need to create modules and use bundletool
        // This is a simplified implementation - real one would create proper module structure
        Log.w(TAG, "AAB building not fully implemented, falling back to APK")
        return false
    }

    private fun createApkFallback(outputApk: File, project: Project): Boolean {
        val manifestFile = File(workspace, "AndroidManifest.xml")
        val resDir = File(workspace, "res")
        val assetsDir = File(workspace, "assets")
        val dexFiles = workspace.walkTopDown().filter { it.extension == "dex" }.toList()

        try {
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                zipOut.setLevel(9)

                // Add manifest
                if (manifestFile.exists()) {
                    addFileToZip(zipOut, manifestFile, "AndroidManifest.xml")
                }

                // Add resources
                if (resDir.exists()) {
                    resDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val entryName = "res/${file.path.substring(resDir.path.length + 1)}"
                        addFileToZip(zipOut, file, entryName)
                    }
                }

                // Add assets
                if (assetsDir.exists()) {
                    assetsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val entryName = "assets/${file.path.substring(assetsDir.path.length + 1)}"
                        addFileToZip(zipOut, file, entryName)
                    }
                }

                // Add DEX files
                dexFiles.forEach { dexFile ->
                    val entryName = "classes${if (dexFile.name == "classes.dex") "" else dexFile.name.removeSuffix(".dex").substringAfterLast("classes")}.dex"
                    addFileToZip(zipOut, dexFile, entryName)
                }

                // Add META-INF/MANIFEST.MF
                addManifestMf(zipOut)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback APK creation failed", e)
            return false
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        zipOut.putNextEntry(ZipEntry(entryName))
        file.copyTo(zipOut)
        zipOut.closeEntry()
    }

    private fun zipDirectory(sourceDir: File, outputZip: File) {
        ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = file.path.substring(sourceDir.path.length + 1)
                zipOut.putNextEntry(ZipEntry(entryName))
                file.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
    }

    private fun addManifestMf(zipOut: ZipOutputStream) {
        val manifestContent = "Manifest-Version: 1.0\nCreated-By: HTML-to-APK Builder\n\n"
        zipOut.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
        zipOut.write(manifestContent.toByteArray())
        zipOut.closeEntry()
    }

    private fun verifyApk(apkFile: File) {
        if (!apkFile.exists()) {
            throw IllegalStateException("APK file was not created")
        }
        if (apkFile.length() < 1024) {
            throw IllegalStateException("APK file is too small (${apkFile.length()} bytes)")
        }
    }
}