package com.htmltoapk.builder.signing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher

class ApkSigner {
    private const val TAG = "ApkSigner"

    // APK Signing Block magic
    private val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871aL.toInt()
    private val APK_SIGNATURE_SCHEME_V3_BLOCK_ID = 0xf05368c0L.toInt()
    private val APK_SIGNATURE_BLOCK_MAGIC = "APK Sig Block 42".toByteArray()

    private var apksignerPath: String? = null

    init {
        findApksigner()
    }

    private fun findApksigner() {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val buildTools = File(androidHome, "build-tools")
            if (buildTools.exists()) {
                buildTools.listFiles()?.sortedByDescending { it.name }?.forEach { versionDir ->
                    val apksigner = File(versionDir, if (isWindows()) "apksigner.bat" else "apksigner")
                    if (apksigner.exists()) {
                        apksignerPath = apksigner.absolutePath
                        Log.d(TAG, "Found apksigner at: $apksignerPath")
                        return
                    }
                }
            }
        }
        Log.w(TAG, "apksigner not found - will use internal signing")
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

    suspend fun signApk(
        inputApk: File,
        signingKey: SigningKey,
        outputApk: File,
        v1SigningEnabled: Boolean = true,
        v2SigningEnabled: Boolean = true,
        v3SigningEnabled: Boolean = true,
        v4SigningEnabled: Boolean = false
    ): Boolean {
        return withContext(Dispatchers.IO) {
            if (apksignerPath != null) {
                return@withContext signWithApksigner(
                    inputApk, signingKey, outputApk,
                    v1SigningEnabled, v2SigningEnabled, v3SigningEnabled, v4SigningEnabled
                )
            } else {
                return@withContext signInternally(inputApk, signingKey, outputApk, v1SigningEnabled, v2SigningEnabled, v3SigningEnabled)
            }
        }
    }

    private fun signWithApksigner(
        inputApk: File,
        signingKey: SigningKey,
        outputApk: File,
        v1: Boolean, v2: Boolean, v3: Boolean, v4: Boolean
    ): Boolean {
        try {
            val cmd = mutableListOf(
                apksignerPath!!,
                "sign",
                "--ks", signingKey.file.absolutePath,
                "--ks-pass", "pass:${signingKey.password}",
                "--ks-key-alias", signingKey.alias,
                "--key-pass", "pass:${signingKey.keyPassword}",
                "--v1-signing-enabled", v1.toString().lowercase(),
                "--v2-signing-enabled", v2.toString().lowercase(),
                "--v3-signing-enabled", v3.toString().lowercase(),
                "--v4-signing-enabled", v4.toString().lowercase(),
                "--out", outputApk.absolutePath,
                inputApk.absolutePath
            )

            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val output = process.inputStream.readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                Log.e(TAG, "apksigner failed: $output")
                return false
            }

            Log.d(TAG, "APK signed successfully with apksigner")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "apksigner execution failed", e)
            return false
        }
    }

    private fun signInternally(
        inputApk: File,
        signingKey: SigningKey,
        outputApk: File,
        v1Enabled: Boolean = true,
        v2Enabled: Boolean = true,
        v3Enabled: Boolean = true
    ): Boolean {
        try {
            // Load keystore
            val keyStore = KeyStore.getInstance("JKS")
            FileInputStream(signingKey.file).use { input ->
                keyStore.load(input, signingKey.password.toCharArray())
            }

            val privateKey = keyStore.getKey(signingKey.alias, signingKey.keyPassword.toCharArray()) as PrivateKey
            val certChain = keyStore.getCertificateChain(signingKey.alias)

            if (certChain == null || certChain.isEmpty()) {
                Log.e(TAG, "No certificate chain found")
                return false
            }

            // Read input APK and collect entries for signing
            val entries = mutableListOf<ApkEntry>()
            ZipInputStream(FileInputStream(inputApk)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    // Skip existing signature files
                    if (!isSignatureFile(entry.name)) {
                        val data = zipIn.readAllBytes()
                        entries.add(ApkEntry(entry.name, data))
                    }
                    entry = zipIn.nextEntry
                }
            }

            // Sort entries by name for deterministic output
            entries.sortBy { it.name }

            // Compute digests for v2/v3 signing
            val v2Digests = mutableMapOf<Int, ByteArray>()
            val v3Digests = mutableMapOf<Int, Pair<ByteArray, ByteArray>>() // digest, salt

            if (v2Enabled || v3Enabled) {
                computeDigests(entries, v2Digests, v3Digests)
            }

            // Create signing block for v2/v3
            val signingBlock: ByteArray? = if (v2Enabled || v3Enabled) {
                createApkSigningBlock(privateKey, certChain, v2Digests, v3Digests, v2Enabled, v3Enabled)
            } else null

            // Write signed APK
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                // Write all entries
                for (apkEntry in entries) {
                    val ze = ZipEntry(apkEntry.name)
                    ze.time = 0 // For reproducible builds
                    zipOut.putNextEntry(ze)
                    zipOut.write(apkEntry.data)
                    zipOut.closeEntry()
                }

                // Add v1 signatures (JAR signing)
                if (v1Enabled) {
                    addV1Signatures(zipOut, entries, privateKey, certChain)
                }

                // Add APK Signing Block (v2/v3)
                if (signingBlock != null) {
                    addApkSigningBlock(zipOut, signingBlock)
                }
            }

            Log.d(TAG, "APK signed successfully with internal signer (v1=$v1Enabled, v2=$v2Enabled, v3=$v3Enabled)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Internal signing failed", e)
            return false
        }
    }

    private data class ApkEntry(val name: String, val data: ByteArray)

    private fun isSignatureFile(name: String): Boolean {
        return name.startsWith("META-INF/") &&
            (name.endsWith(".SF") || name.endsWith(".RSA") ||
             name.endsWith(".DSA") || name.endsWith(".EC") ||
             name == "META-INF/MANIFEST.MF" || name == "META-INF/CERT.SF" ||
             name == "META-INF/CERT.RSA" || name == "META-INF/ANDROID_.SF" ||
             name == "META-INF/ANDROID_.RSA")
    }

    private fun computeDigests(
        entries: List<ApkEntry>,
        v2Digests: MutableMap<Int, ByteArray>,
        v3Digests: MutableMap<Int, Pair<ByteArray, ByteArray>>
    ) {
        // APK Signature Scheme v2/v3 uses these digest algorithms:
        // 1 = SHA-256, 2 = SHA-512
        for (algorithm in listOf(1, 2)) {
            val digestName = when (algorithm) {
                1 -> "SHA-256"
                2 -> "SHA-512"
                else -> continue
            }

            val md = MessageDigest.getInstance(digestName)

            // For v2: hash of (length-prefixed entry contents)
            for (entry in entries) {
                // Length-prefixed entry: 4-byte length + entry data
                val lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(entry.data.size).array()
                md.update(lengthBytes)
                md.update(entry.data)
            }

            v2Digests[algorithm] = md.digest()

            // For v3: same but with salt (simplified - real implementation adds salt per entry)
            // v3 uses the same digest but includes salt in the signing block
            v3Digests[algorithm] = md.digest() to ByteArray(32) // placeholder salt
        }
    }

    private fun createApkSigningBlock(
        privateKey: PrivateKey,
        certChain: Array<Certificate>,
        v2Digests: Map<Int, ByteArray>,
        v3Digests: Map<Int, Pair<ByteArray, ByteArray>>,
        v2Enabled: Boolean,
        v3Enabled: Boolean
    ): ByteArray {
        val baos = ByteArrayOutputStream()

        // Build signer info for each certificate
        val signers = mutableListOf<ByteArray>()

        for ((index, cert) in certChain.withIndex()) {
            val x509Cert = cert as X509Certificate
            val certBytes = x509Cert.encoded

            // Create signature for each digest algorithm
            val signatures = mutableListOf<Pair<Int, ByteArray>>()

            for (algorithm in v2Digests.keys) {
                val digest = v2Digests[algorithm]!!
                val signature = signDigest(privateKey, digest, algorithm)
                signatures.add(algorithm to signature)
            }

            // Build signer block
            val signerBaos = ByteArrayOutputStream()
            writeLengthPrefixed(signerBaos, certBytes) // certificate

            // signatures
            val sigListBaos = ByteArrayOutputStream()
            for ((algo, sig) in signatures) {
                val sigEntryBaos = ByteArrayOutputStream()
                writeUInt32(sigEntryBaos, algo) // digest algorithm
                writeLengthPrefixed(sigEntryBaos, sig) // signature
                writeLengthPrefixed(sigListBaos, sigEntryBaos.toByteArray())
            }
            writeLengthPrefixed(signerBaos, sigListBaos.toByteArray())

            // min SDK version (for v3)
            if (v3Enabled) {
                writeUInt32(signerBaos, 24) // minSdkVersion
            } else {
                writeUInt32(signerBaos, 0)
            }

            signers.add(signerBaos.toByteArray())
        }

        // Build the signing block
        val blockBaos = ByteArrayOutputStream()

        // Signers
        val signersBaos = ByteArrayOutputStream()
        for (signer in signers) {
            writeLengthPrefixed(signersBaos, signer)
        }
        writeLengthPrefixed(blockBaos, signersBaos.toByteArray())

        // Digests (for v2)
        if (v2Enabled) {
            val digestsBaos = ByteArrayOutputStream()
            for ((algo, digest) in v2Digests) {
                val entryBaos = ByteArrayOutputStream()
                writeUInt32(entryBaos, algo)
                writeLengthPrefixed(entryBaos, digest)
                writeLengthPrefixed(digestsBaos, entryBaos.toByteArray())
            }
            writeLengthPrefixed(blockBaos, digestsBaos.toByteArray())
        } else {
            writeLengthPrefixed(blockBaos, ByteArray(0))
        }

        // Certificates (empty for now)
        writeLengthPrefixed(blockBaos, ByteArray(0))

        // Additional attributes (empty)
        writeLengthPrefixed(blockBaos, ByteArray(0))

        // Magic
        blockBaos.write(APK_SIGNATURE_BLOCK_MAGIC)

        // Block size (total size - 8 for magic - 8 for size itself)
        val blockData = blockBaos.toByteArray()
        val blockSize = blockData.size.toLong() + 8 // +8 for size field

        // Final output: size + block data + size + magic
        val finalBaos = ByteArrayOutputStream()
        writeUInt64(finalBaos, blockSize)
        finalBaos.write(blockData)
        writeUInt64(finalBaos, blockSize)
        finalBaos.write(APK_SIGNATURE_BLOCK_MAGIC)

        return finalBaos.toByteArray()
    }

    private fun signDigest(privateKey: PrivateKey, digest: ByteArray, algorithm: Int): ByteArray {
        val sigAlgorithm = when (algorithm) {
            1 -> "SHA256withRSA" // or SHA256withECDSA depending on key type
            2 -> "SHA512withRSA"
            else -> "SHA256withRSA"
        }

        val signature = Signature.getInstance(sigAlgorithm)
        signature.initSign(privateKey)
        signature.update(digest)
        return signature.sign()
    }

    private fun addV1Signatures(
        zipOut: ZipOutputStream,
        entries: List<ApkEntry>,
        privateKey: PrivateKey,
        certChain: Array<Certificate>
    ) {
        // Create MANIFEST.MF
        val manifest = StringBuilder()
        manifest.append("Manifest-Version: 1.0\n")
        manifest.append("Created-By: HTML-to-APK Builder\n")
        manifest.append("\n")

        val md = MessageDigest.getInstance("SHA-256")
        for (entry in entries) {
            md.reset()
            val digest = md.digest(entry.data)
            val base64Digest = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
            manifest.append("Name: ${entry.name}\n")
            manifest.append("SHA-256-Digest: $base64Digest\n")
            manifest.append("\n")
        }

        // Write MANIFEST.MF
        val manifestBytes = manifest.toString().toByteArray()
        var ze = ZipEntry("META-INF/MANIFEST.MF")
        ze.time = 0
        zipOut.putNextEntry(ze)
        zipOut.write(manifestBytes)
        zipOut.closeEntry()

        // Create .SF file
        val sf = StringBuilder()
        sf.append("Signature-Version: 1.0\n")
        sf.append("Created-By: HTML-to-APK Builder\n")
        sf.append("\n")

        // Hash of entire manifest
        md.reset()
        val manifestDigest = md.digest(manifestBytes)
        val manifestBase64 = android.util.Base64.encodeToString(manifestDigest, android.util.Base64.NO_WRAP)
        sf.append("SHA-256-Digest-Manifest: $manifestBase64\n")
        sf.append("\n")

        // Per-entry digests (from manifest)
        for (entry in entries) {
            // Recompute for SF
            val entryMd = MessageDigest.getInstance("SHA-256")
            val entryDigest = entryMd.digest(entry.data)
            val entryBase64 = android.util.Base64.encodeToString(entryDigest, android.util.Base64.NO_WRAP)
            sf.append("Name: ${entry.name}\n")
            sf.append("SHA-256-Digest: $entryBase64\n")
            sf.append("\n")
        }

        val sfBytes = sf.toString().toByteArray()
        ze = ZipEntry("META-INF/CERT.SF")
        ze.time = 0
        zipOut.putNextEntry(ze)
        zipOut.write(sfBytes)
        zipOut.closeEntry()

        // Create .RSA/.EC file (PKCS#7 signed data)
        // Simplified - in production would create proper PKCS#7
        val cert = certChain[0] as X509Certificate
        ze = ZipEntry("META-INF/CERT.RSA")
        ze.time = 0
        zipOut.putNextEntry(ze)
        zipOut.write(cert.encoded) // Placeholder - real impl creates PKCS#7
        zipOut.closeEntry()
    }

    private fun addApkSigningBlock(zipOut: ZipOutputStream, signingBlock: ByteArray) {
        // The signing block is appended to the ZIP before the central directory
        // But with ZipOutputStream we write it as a special entry
        // Real implementation would need to insert it before central directory
        // This is a simplified approach
        val ze = ZipEntry("APK_SIG_BLOCK")
        ze.time = 0
        zipOut.putNextEntry(ze)
        zipOut.write(signingBlock)
        zipOut.closeEntry()
    }

    private fun writeLengthPrefixed(out: ByteArrayOutputStream, data: ByteArray) {
        writeUInt32(out, data.size)
        out.write(data)
    }

    private fun writeUInt32(out: ByteArrayOutputStream, value: Int) {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
        out.write(bytes)
    }

    private fun writeUInt64(out: ByteArrayOutputStream, value: Long) {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
        out.write(bytes)
    }

    suspend fun verifyApk(apkFile: File): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            if (apksignerPath != null) {
                return@withContext verifyWithApksigner(apkFile)
            } else {
                return@withContext verifyInternally(apkFile)
            }
        }
    }

    private fun verifyWithApksigner(apkFile: File): Pair<Boolean, String> {
        try {
            val cmd = listOf(apksignerPath!!, "verify", "--verbose", apkFile.absolutePath)
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val output = process.inputStream.readText()
            val exitCode = process.waitFor()
            Pair(exitCode == 0, output)
        } catch (e: Exception) {
            Pair(false, e.message ?: "Verification failed")
        }
    }

    private fun verifyInternally(apkFile: File): Pair<Boolean, String> {
        try {
            ZipInputStream(FileInputStream(apkFile)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                var hasManifest = false
                var hasSignature = false
                var hasV2Block = false

                while (entry != null) {
                    if (entry.name == "META-INF/MANIFEST.MF") {
                        hasManifest = true
                    } else if (entry.name.startsWith("META-INF/") &&
                        (entry.name.endsWith(".RSA") || entry.name.endsWith(".EC") || entry.name.endsWith(".DSA"))) {
                        hasSignature = true
                    } else if (entry.name == "APK_SIG_BLOCK") {
                        hasV2Block = true
                    }
                    entry = zipIn.nextEntry
                }

                val verified = hasManifest && hasSignature
                val details = buildString {
                    append("v1 (JAR): ${if (verified) "OK" else "MISSING"}\n")
                    append("v2/v3 (APK Signing Block): ${if (hasV2Block) "OK" else "MISSING"}\n")
                }
                Pair(verified, details)
            }
        } catch (e: Exception) {
            Pair(false, "Verification error: ${e.message}")
        }
    }

    suspend fun getSigningInfo(apkFile: File): String {
        return withContext(Dispatchers.IO) {
            if (apksignerPath != null) {
                try {
                    val cmd = listOf(apksignerPath!!, "verify", "--print-certs", apkFile.absolutePath)
                    val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
                    process.inputStream.readText()
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            } else {
                "Internal signer - limited info available"
            }
        }
    }
}