package com.htmltoapk.builder.signing

import android.content.Context
import android.util.Log
import com.htmltoapk.builder.data.KeystoreEntity
import com.htmltoapk.builder.HtmlToApkApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SigningKeyResolver {
    private const val TAG = "SigningKeyResolver"

    suspend fun resolve(
        context: Context,
        buildConfig: com.htmltoapk.builder.model.BuildConfiguration
    ): SigningKey? {
        return withContext(Dispatchers.IO) {
            if (buildConfig.useCustomKeystore && buildConfig.keystorePath.isNotBlank()) {
                return@withContext SigningKey.fromKeystore(
                    buildConfig.keystorePath,
                    buildConfig.keystorePassword,
                    buildConfig.keyAlias,
                    buildConfig.keyPassword
                )
            }

            // Try to find default keystore
            findDefaultKeystore(context)
        }
    }

    suspend fun resolveKeystore(keystoreId: Long): SigningKey? {
        return withContext(Dispatchers.IO) {
            val db = HtmlToApkApplication.getInstance().getDatabase()
            val keystore = db.keystoreDao().getKeystoreById(keystoreId)
            if (keystore != null) {
                return@withContext SigningKey.fromKeystore(
                    keystore.filePath,
                    keystore.password,
                    keystore.alias,
                    keystore.keyPassword
                )
            }
            null
        }
    }

    suspend fun getDefaultKeystore(context: Context): SigningKey? {
        return withContext(Dispatchers.IO) {
            val db = HtmlToApkApplication.getInstance().getDatabase()
            val keystore = db.keystoreDao().getDefaultKeystore()
            if (keystore != null) {
                return@withContext SigningKey.fromKeystore(
                    keystore.filePath,
                    keystore.password,
                    keystore.alias,
                    keystore.keyPassword
                )
            }
            // Fallback to file-based search
            findDefaultKeystore(context)
        }
    }

    private suspend fun findDefaultKeystore(context: Context): SigningKey? {
        return withContext(Dispatchers.IO) {
            val projectDir = context.filesDir.parentFile
            val candidates = listOf(
                File(projectDir, "keystore.jks"),
                File(projectDir, "release.keystore"),
                File(projectDir, "debug.keystore"),
                File(projectDir, "app.jks"),
                File(projectDir, "upload-keystore.jks")
            )

            candidates.forEach { candidate ->
                if (candidate.exists()) {
                    // Try common passwords
                    listOf("android", "password", "123456", "").forEach { password ->
                        try {
                            val keyStore = java.security.KeyStore.getInstance("JKS")
                            java.io.FileInputStream(candidate).use { input ->
                                keyStore.load(input, password.toCharArray())
                            }

                            // Get first alias
                            val aliases = keyStore.aliases()
                            if (aliases.hasMoreElements()) {
                                val alias = aliases.nextElement()
                                return@withContext SigningKey.fromKeystore(
                                    candidate.absolutePath,
                                    password,
                                    alias,
                                    password
                                )
                            }
                        } catch (e: Exception) {
                            // Try next password
                        }
                    }
                }
            }

            null
        }
    }
}