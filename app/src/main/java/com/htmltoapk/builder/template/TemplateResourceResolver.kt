package com.htmltoapk.builder.template

import android.util.Log
import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.model.WebViewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object TemplateResourceResolver {
    private const val TAG = "TemplateResourceResolver"

    private val PLACEHOLDERS = mapOf(
        "PACKAGE_NAME" to "com.template.app",
        "APP_NAME" to "TemplateApp",
        "VERSION_CODE" to "1",
        "VERSION_NAME" to "1.0",
        "MIN_SDK" to "24",
        "TARGET_SDK" to "34",
        "ORIENTATION" to "portrait",
        "THEME_COLOR" to "#6200EE",
        "ICON_PATH" to "@mipmap/ic_launcher",
        "SPLASH_PATH" to "@drawable/splash",
        "PERMISSIONS" to "",
        "ADMOB_APP_ID" to "",
        "BILLING_ENABLED" to "false",
        "WEBVIEW_SETTINGS" to "{}",
        "DEEP_LINK_SCHEME" to ""
    )

    fun resolveTemplate(project: Project, templateDir: File, outputDir: File): Boolean {
        return try {
            val replacementMap = buildReplacementMap(project)
            copyAndResolve(templateDir, outputDir, replacementMap)
            processManifest(outputDir, project)
            processStrings(outputDir, project)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve template", e)
            false
        }
    }

    private fun buildReplacementMap(project: Project): Map<String, String> {
        val permissionsXml = generatePermissionsXml(project.permissions)
        val webviewJson = Json { prettyPrint = true }.encodeToString(WebViewSettings.serializer(), project.webViewSettings)

        return mapOf(
            "PACKAGE_NAME" to project.packageName,
            "APP_NAME" to project.appName.ifBlank { project.name },
            "VERSION_CODE" to project.versionCode.toString(),
            "VERSION_NAME" to project.versionName,
            "MIN_SDK" to project.minSdk.toString(),
            "TARGET_SDK" to project.targetSdk.toString(),
            "ORIENTATION" to project.orientation,
            "THEME_COLOR" to project.primaryColor,
            "ICON_PATH" to project.iconPath.ifBlank { "@mipmap/ic_launcher" },
            "SPLASH_PATH" to project.splashPath.ifBlank { "@drawable/splash" },
            "PERMISSIONS" to permissionsXml,
            "ADMOB_APP_ID" to project.admob.appId,
            "BILLING_ENABLED" to project.billing.enabled.toString().lowercase(),
            "WEBVIEW_SETTINGS" to webviewJson,
            "DEEP_LINK_SCHEME" to project.deepLinkScheme
        )
    }

    private fun generatePermissionsXml(permissions: List<String>): String {
        if (permissions.isEmpty()) return ""
        return permissions.joinToString("\n") { "    <uses-permission android:name=\"$it\" />" }
    }

    private fun copyAndResolve(
        srcDir: File,
        destDir: File,
        replacements: Map<String, String>
    ) {
        if (!srcDir.exists()) return

        destDir.mkdirs()

        srcDir.walkTopDown().forEach { srcFile ->
            val relativePath = srcFile.path.substring(srcDir.path.length + 1)
            val destFile = File(destDir, relativePath)

            if (srcFile.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                processFile(srcFile, destFile, replacements)
            }
        }
    }

    private fun processFile(srcFile: File, destFile: File, replacements: Map<String, String>) {
        val textExtensions = setOf("xml", "json", "txt", "properties", "gradle", "kt", "java", "html", "css", "js", "manifest")
        val extension = srcFile.extension.lowercase()

        if (extension in textExtensions) {
            // Text file - resolve placeholders
            val content = srcFile.readText()
            var resolved = content
            replacements.forEach { (placeholder, value) ->
                resolved = resolved
                    .replace("{{$placeholder}}", value)
                    .replace("@$placeholder@", value)
                    .replace("\${$placeholder}", value)
            }
            FileWriter(destFile).use { it.write(resolved) }
        } else {
            // Binary file - copy as-is
            srcFile.copyTo(destFile, overwrite = true)
        }
    }

    private fun processManifest(outputDir: File, project: Project) {
        val manifestFile = File(outputDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) return

        try {
            // Simple XML processing for manifest
            val content = manifestFile.readText()
            var modified = content
                .replace("package=\"com.template.app\"", "package=\"${project.packageName}\"")
                .replace("android:versionCode=\"1\"", "android:versionCode=\"${project.versionCode}\"")
                .replace("android:versionName=\"1.0\"", "android:versionName=\"${project.versionName}\"")

            // Add permissions if not present
            val permissionsXml = generatePermissionsXml(project.permissions)
            if (permissionsXml.isNotBlank() && !content.contains("<uses-permission")) {
                // Insert after package declaration
                modified = modified.replaceFirst(
                    "(package=\"[^\"]+\")",
                    "$1\n$permissionsXml"
                )
            }

            manifestFile.writeText(modified)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to process manifest", e)
        }
    }

    private fun processStrings(outputDir: File, project: Project) {
        val stringsFile = File(outputDir, "res/values/strings.xml")
        if (!stringsFile.exists()) return

        try {
            val content = stringsFile.readText()
            val modified = content.replace(
                "<string name=\"app_name\">TemplateApp</string>",
                "<string name=\"app_name\">${project.appName.ifBlank { project.name }}</string>"
            )
            stringsFile.writeText(modified)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to process strings", e)
        }
    }
}