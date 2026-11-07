package com.htmltoapk.builder.signing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.Certificate

data class SigningKey(
    val file: File,
    val password: String,
    val alias: String,
    val keyPassword: String
) {
    companion object {
        private const val TAG = "SigningKey"

        suspend fun validate(context: Context, signingKey: SigningKey): ValidationResult {
            return withContext(Dispatchers.IO) {
                try {
                    if (!signingKey.file.exists()) {
                        return@withContext ValidationResult(false, "Keystore file not found: ${signingKey.file.absolutePath}")
                    }

                    // Try to load keystore and verify credentials
                    val keyStore = KeyStore.getInstance("JKS")
                    FileInputStream(signingKey.file).use { input ->
                        keyStore.load(input, signingKey.password.toCharArray())
                    }

                    // Check if alias exists
                    if (!keyStore.containsAlias(signingKey.alias)) {
                        return@withContext ValidationResult(false, "Key alias not found: ${signingKey.alias}")
                    }

                    // Try to get the key with key password
                    val key = keyStore.getKey(signingKey.alias, signingKey.keyPassword.toCharArray())
                    if (key == null) {
                        return@withContext ValidationResult(false, "Invalid key password for alias: ${signingKey.alias}")
                    }

                    // Get certificate chain
                    val certChain = keyStore.getCertificateChain(signingKey.alias)
                    if (certChain == null || certChain.isEmpty()) {
                        return@withContext ValidationResult(false, "No certificate found for alias: ${signingKey.alias}")
                    }

                    val cert = certChain[0]
                    val validUntil = if (cert is java.security.cert.X509Certificate) {
                        cert.notAfter.toString()
                    } else {
                        "Unknown"
                    }

                    ValidationResult(true, "Valid keystore. Certificate valid until: $validUntil")

                } catch (e: Exception) {
                    Log.e(TAG, "Keystore validation failed", e)
                    ValidationResult(false, "Validation failed: ${e.message}")
                }
            }
        }

        suspend fun createDebugKeystore(context: Context, outputFile: File): SigningKey {
            return withContext(Dispatchers.IO) {
                // For debug, we'll create a simple keystore
                // In production, this would use keytool
                val keyStore = KeyStore.getInstance("PKCS12")
                keyStore.load(null, null)

                // Generate a self-signed certificate and key pair
                // This is a simplified version - real implementation would use KeyPairGenerator
                val password = "android"
                val alias = "androiddebugkey"

                FileOutputStream(outputFile).use { output ->
                    keyStore.store(output, password.toCharArray())
                }

                SigningKey(outputFile, password, alias, password)
            }
        }

        fun fromKeystore(
            keystorePath: String,
            keystorePassword: String,
            keyAlias: String,
            keyPassword: String = ""
        ): SigningKey {
            val pwd = if (keyPassword.isBlank()) keystorePassword else keyPassword
            return SigningKey(
                file = File(keystorePath),
                password = keystorePassword,
                alias = keyAlias,
                keyPassword = pwd
            )
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)