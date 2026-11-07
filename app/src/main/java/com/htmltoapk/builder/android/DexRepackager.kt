package com.htmltoapk.builder.android

import android.util.Log
import org.jf.baksmali.Baksmali
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.writer.DexFileFactory
import org.jf.smali.Smali
import org.jf.util.ConsoleUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class DexRepackager {
    private const val TAG = "DexRepackager"

    companion object {
        @Volatile
        private var INSTANCE: DexRepackager? = null

        fun getInstance(): DexRepackager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DexRepackager().also { INSTANCE = it }
            }
        }
    }

    fun rewriteDexDirectory(dexDir: File, oldPackage: String, newPackage: String) {
        val oldPath = oldPackage.replace(".", "/")
        val newPath = newPackage.replace(".", "/")

        Log.d(TAG, "Rewriting DEX files: $oldPackage -> $newPackage")

        val dexFiles = dexDir.walkTopDown().filter { it.extension == "dex" }.toList()

        for (dexFile in dexFiles) {
            try {
                rewriteDexFile(dexFile, oldPackage, newPackage)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to rewrite DEX file: ${dexFile.name}, falling back to simple replacement", e)
                rewriteDexFileSimple(dexFile, oldPath, newPath)
            }
        }

        // Also process smali files if present
        dexDir.walkTopDown().filter { it.extension == "smali" }.forEach { smaliFile ->
            rewriteSmaliFile(smaliFile, oldPackage, newPackage)
        }
    }

    private fun rewriteDexFile(dexFile: File, oldPackage: String, newPackage: String) {
        // Create temp directory for baksmali output
        val tempDir = Files.createTempDirectory("dex_repack_${dexFile.name}_").toFile()

        try {
            // Step 1: Disassemble DEX to smali using baksmali
            val baksmaliArgs = arrayOf(
                "disassemble",
                "--output", tempDir.absolutePath,
                dexFile.absolutePath
            )

            val baksmaliExitCode = runBaksmali(baksmaliArgs)
            if (baksmaliExitCode != 0) {
                throw IOException("baksmali failed with exit code $baksmaliExitCode")
            }

            // Step 2: Rewrite package references in smali files
            rewriteSmaliDirectory(tempDir, oldPackage, newPackage)

            // Step 3: Reassemble smali back to DEX using smali
            val outputDexFile = File(dexDir, "${dexFile.name.stem}_repacked.dex")
            val smaliArgs = arrayOf(
                "assemble",
                "--output", outputDexFile.absolutePath,
                "--api", "24",
                tempDir.absolutePath
            )

            val smaliExitCode = runSmali(smaliArgs)
            if (smaliExitCode != 0) {
                throw IOException("smali failed with exit code $smaliExitCode")
            }

            // Step 4: Replace original DEX with repacked one
            if (outputDexFile.exists() && outputDexFile.length() > 0) {
                outputDexFile.renameTo(dexFile)
                Log.d(TAG, "Successfully repacked DEX file: ${dexFile.name}")
            } else {
                throw IOException("Repacked DEX file is empty")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Failed to repack DEX with baksmali/smali: ${dexFile.name}", e)
            throw e
        } finally {
            // Cleanup temp directory
            deleteRecursive(tempDir)
        }
    }

    private fun rewriteDexFileSimple(dexFile: File, oldPath: String, newPath: String) {
        try {
            // Simple string replacement in DEX (fallback)
            val bytes = dexFile.readBytes()
            val content = String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)
            val modified = content.replace(oldPath, newPath)

            if (modified != content) {
                FileOutputStream(dexFile).use { it.write(modified.toByteArray(java.nio.charset.StandardCharsets.ISO_8859_1)) }
                Log.d(TAG, "Rewrote DEX file (simple): ${dexFile.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to rewrite DEX file (simple): ${dexFile.name}", e)
        }
    }

    private fun rewriteSmaliFile(smaliFile: File, oldPackage: String, newPackage: String) {
        try {
            val content = smaliFile.readText()
            val modified = content
                .replace(oldPackage, newPackage)
                .replace(oldPackage.replace(".", "/"), newPackage.replace(".", "/"))

            if (modified != content) {
                smaliFile.writeText(modified)
                Log.d(TAG, "Rewrote smali file: ${smaliFile.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to rewrite smali file: ${smaliFile.name}", e)
        }
    }

    private fun rewriteSmaliDirectory(smaliDir: File, oldPackage: String, newPackage: String) {
        val oldPath = oldPackage.replace(".", "/")
        val newPath = newPackage.replace(".", "/")

        smaliDir.walkTopDown()
            .filter { it.extension == "smali" }
            .forEach { smaliFile ->
                try {
                    val content = smaliFile.readText()
                    var modified = content
                        .replace(oldPackage, newPackage)
                        .replace(oldPath, newPath)

                    // Also handle L prefix in smali (e.g., Lcom/example/App;)
                    val oldClassPrefix = "L$oldPath/"
                    val newClassPrefix = "L$newPath/"
                    modified = modified.replace(oldClassPrefix, newClassPrefix)

                    val oldClassEnd = "L$oldPath;"
                    val newClassEnd = "L$newPath;"
                    modified = modified.replace(oldClassEnd, newClassEnd)

                    if (modified != content) {
                        smaliFile.writeText(modified)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to rewrite smali file: ${smaliFile.name}", e)
                }
            }
    }

    private fun runBaksmali(args: Array<String>): Int {
        // Redirect System.out and System.err to capture output
        val originalOut = System.out
        val originalErr = System.err
        val outBuffer = java.io.ByteArrayOutputStream()
        val errBuffer = java.io.ByteArrayOutputStream()

        try {
            System.setOut(java.io.PrintStream(outBuffer))
            System.setErr(java.io.PrintStream(errBuffer))
            return Baksmali.main(args)
        } catch (e: Exception) {
            Log.e(TAG, "baksmali exception: ${e.message}")
            return -1
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            val out = outBuffer.toString()
            val err = errBuffer.toString()
            if (out.isNotBlank()) Log.d(TAG, "baksmali stdout: $out")
            if (err.isNotBlank()) Log.w(TAG, "baksmali stderr: $err")
        }
    }

    private fun runSmali(args: Array<String>): Int {
        val originalOut = System.out
        val originalErr = System.err
        val outBuffer = java.io.ByteArrayOutputStream()
        val errBuffer = java.io.ByteArrayOutputStream()

        try {
            System.setOut(java.io.PrintStream(outBuffer))
            System.setErr(java.io.PrintStream(errBuffer))
            return Smali.main(args)
        } catch (e: Exception) {
            Log.e(TAG, "smali exception: ${e.message}")
            return -1
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            val out = outBuffer.toString()
            val err = errBuffer.toString()
            if (out.isNotBlank()) Log.d(TAG, "smali stdout: $out")
            if (err.isNotBlank()) Log.w(TAG, "smali stderr: $err")
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}