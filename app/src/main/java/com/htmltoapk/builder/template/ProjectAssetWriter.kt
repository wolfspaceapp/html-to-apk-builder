package com.htmltoapk.builder.template

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.model.WebViewSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream

object ProjectAssetWriter {
    private const val TAG = "ProjectAssetWriter"
    private val json = Json { prettyPrint = true }

    suspend fun writeProjectAssets(
        context: Context,
        project: Project,
        templateDir: File,
        includeSource: Boolean = true
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val assetsDir = File(templateDir, "assets")
                assetsDir.mkdirs()

                // Write config.json
                writeConfig(assetsDir, project)

                // Write web view settings
                writeWebViewSettings(assetsDir, project)

                // Write source files
                if (includeSource) {
                    writeSourceFiles(context, project, assetsDir)
                }

                // Copy icons
                copyIcons(context, project, templateDir)

                // Copy splash screens
                copySplash(context, project, templateDir)

                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write project assets", e)
                false
            }
        }
    }

    private fun writeConfig(assetsDir: File, project: Project) {
        // Build JsonObject using Map<String, JsonElement>
        val configMap = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()

        configMap["packageName"] = JsonPrimitive(project.packageName)
        configMap["appName"] = JsonPrimitive(project.appName.ifBlank { project.name })
        configMap["versionCode"] = JsonPrimitive(project.versionCode)
        configMap["versionName"] = JsonPrimitive(project.versionName)
        configMap["orientation"] = JsonPrimitive(project.orientation)
        configMap["themeColor"] = JsonPrimitive(project.primaryColor)
        configMap["showActionBar"] = JsonPrimitive(project.showActionBar)
        configMap["showHeader"] = JsonPrimitive(project.showHeader)
        configMap["showWatermark"] = JsonPrimitive(project.showWatermark)
        configMap["keepScreenOn"] = JsonPrimitive(project.keepScreenOn)
        configMap["desktopMode"] = JsonPrimitive(project.desktopMode)
        configMap["deepLinkScheme"] = JsonPrimitive(project.deepLinkScheme)
        configMap["permissions"] = JsonPrimitive(project.permissions.joinToString(","))
        configMap["webViewSettings"] = json.toJsonElement(project.webViewSettings)
        configMap["admob"] = buildAdMobJson(project.admob)
        configMap["unityAds"] = buildUnityAdsJson(project.unityAds)
        configMap["startApp"] = buildStartAppJson(project.startApp)
        configMap["billing"] = buildBillingJson(project.billing)
        configMap["ump"] = buildUmpJson(project.ump)

        val config = JsonObject(configMap)

        val configFile = File(assetsDir, "config.json")
        FileWriter(configFile).use { it.write(json.encodeToString(config)) }
    }

    private fun buildAdMobJson(admob: com.htmltoapk.builder.model.AdMobSettings): JsonObject {
        val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        map["enabled"] = JsonPrimitive(admob.enabled)
        map["appId"] = JsonPrimitive(admob.appId)
        map["bannerEnabled"] = JsonPrimitive(admob.bannerEnabled)
        map["bannerUnitId"] = JsonPrimitive(admob.bannerUnitId)
        map["bannerPosition"] = JsonPrimitive(admob.bannerPosition)
        map["showBannerOnStart"] = JsonPrimitive(admob.showBannerOnStart)
        map["interstitialEnabled"] = JsonPrimitive(admob.interstitialEnabled)
        map["interstitialUnitId"] = JsonPrimitive(admob.interstitialUnitId)
        map["interstitialIntervalSeconds"] = JsonPrimitive(admob.interstitialIntervalSeconds)
        map["interstitialPageCount"] = JsonPrimitive(admob.interstitialPageCount)
        map["rewardedEnabled"] = JsonPrimitive(admob.rewardedEnabled)
        map["rewardedUnitId"] = JsonPrimitive(admob.rewardedUnitId)
        map["autoLoadInterstitial"] = JsonPrimitive(admob.autoLoadInterstitial)
        map["autoLoadRewarded"] = JsonPrimitive(admob.autoLoadRewarded)
        map["useTestAds"] = JsonPrimitive(admob.useTestAds)
        map["testDeviceIds"] = JsonPrimitive(admob.testDeviceIds)
        map["maxAdContentRating"] = JsonPrimitive(admob.maxAdContentRating)
        map["childDirectedTreatment"] = JsonPrimitive(admob.childDirectedTreatment)
        map["underAgeOfConsent"] = JsonPrimitive(admob.underAgeOfConsent)
        return JsonObject(map)
    }

    private fun buildUnityAdsJson(unityAds: com.htmltoapk.builder.model.UnityAdsSettings): JsonObject {
        val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        map["enabled"] = JsonPrimitive(unityAds.enabled)
        map["gameId"] = JsonPrimitive(unityAds.gameId)
        map["testMode"] = JsonPrimitive(unityAds.testMode)
        map["bannerEnabled"] = JsonPrimitive(unityAds.bannerEnabled)
        map["bannerPlacementId"] = JsonPrimitive(unityAds.bannerPlacementId)
        map["bannerPosition"] = JsonPrimitive(unityAds.bannerPosition)
        map["interstitialEnabled"] = JsonPrimitive(unityAds.interstitialEnabled)
        map["interstitialPlacementId"] = JsonPrimitive(unityAds.interstitialPlacementId)
        map["interstitialIntervalSeconds"] = JsonPrimitive(unityAds.interstitialIntervalSeconds)
        map["interstitialPageCount"] = JsonPrimitive(unityAds.interstitialPageCount)
        map["rewardedEnabled"] = JsonPrimitive(unityAds.rewardedEnabled)
        map["rewardedPlacementId"] = JsonPrimitive(unityAds.rewardedPlacementId)
        map["autoLoadInterstitial"] = JsonPrimitive(unityAds.autoLoadInterstitial)
        map["autoLoadRewarded"] = JsonPrimitive(unityAds.autoLoadRewarded)
        map["consentRequired"] = JsonPrimitive(unityAds.consentRequired)
        map["metadata"] = JsonPrimitive(unityAds.metadata)
        return JsonObject(map)
    }

    private fun buildStartAppJson(startApp: com.htmltoapk.builder.model.StartAppSettings): JsonObject {
        val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        map["enabled"] = JsonPrimitive(startApp.enabled)
        map["appId"] = JsonPrimitive(startApp.appId)
        map["devId"] = JsonPrimitive(startApp.devId)
        map["testMode"] = JsonPrimitive(startApp.testMode)
        map["bannerEnabled"] = JsonPrimitive(startApp.bannerEnabled)
        map["bannerType"] = JsonPrimitive(startApp.bannerType)
        map["bannerPosition"] = JsonPrimitive(startApp.bannerPosition)
        map["interstitialEnabled"] = JsonPrimitive(startApp.interstitialEnabled)
        map["interstitialIntervalSeconds"] = JsonPrimitive(startApp.interstitialIntervalSeconds)
        map["interstitialPageCount"] = JsonPrimitive(startApp.interstitialPageCount)
        map["rewardedEnabled"] = JsonPrimitive(startApp.rewardedEnabled)
        map["splashEnabled"] = JsonPrimitive(startApp.splashEnabled)
        map["splashTheme"] = JsonPrimitive(startApp.splashTheme)
        map["splashOrientation"] = JsonPrimitive(startApp.splashOrientation)
        map["autoLoadInterstitial"] = JsonPrimitive(startApp.autoLoadInterstitial)
        map["autoLoadRewarded"] = JsonPrimitive(startApp.autoLoadRewarded)
        map["preloadSplash"] = JsonPrimitive(startApp.preloadSplash)
        map["consentRequired"] = JsonPrimitive(startApp.consentRequired)
        map["metadata"] = JsonPrimitive(startApp.metadata)
        return JsonObject(map)
    }

    private fun buildBillingJson(billing: com.htmltoapk.builder.model.BillingSettings): JsonObject {
        val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        map["enabled"] = JsonPrimitive(billing.enabled)
        map["type"] = JsonPrimitive(billing.type)
        map["productId"] = JsonPrimitive(billing.productId)
        map["productName"] = JsonPrimitive(billing.productName)
        map["productDescription"] = JsonPrimitive(billing.productDescription)
        map["subscriptionId"] = JsonPrimitive(billing.subscriptionId)
        map["subscriptionPeriod"] = JsonPrimitive(billing.subscriptionPeriod)
        map["freeTrialDays"] = JsonPrimitive(billing.freeTrialDays)
        map["introductoryPrice"] = JsonPrimitive(billing.introductoryPrice)
        map["acknowledge"] = JsonPrimitive(billing.acknowledge)
        map["autoConsume"] = JsonPrimitive(billing.autoConsume)
        map["enablePendingPurchases"] = JsonPrimitive(billing.enablePendingPurchases)
        map["testMode"] = JsonPrimitive(billing.testMode)
        return JsonObject(map)
    }

    private fun buildUmpJson(ump: com.htmltoapk.builder.model.UMPSettings): JsonObject {
        val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        map["debugEnabled"] = JsonPrimitive(ump.debugEnabled)
        map["debugGeography"] = JsonPrimitive(ump.debugGeography)
        map["testDeviceIds"] = JsonPrimitive(ump.testDeviceIds)
        return JsonObject(map)
    }

    private fun writeWebViewSettings(assetsDir: File, project: Project) {
        val settingsFile = File(assetsDir, "webview_settings.json")
        FileWriter(settingsFile).use { it.write(json.encodeToString(project.webViewSettings)) }
    }

    private fun writeSourceFiles(context: Context, project: Project, assetsDir: File) {
        val wwwDir = File(assetsDir, "www")
        wwwDir.mkdirs()

        when (project.contentSource) {
            is Project.ContentSource.Local -> {
                val sourcePath = project.sourcePath.ifBlank { project.contentSource.path }
                if (sourcePath.isNotBlank()) {
                    copySourceFiles(context, sourcePath, wwwDir)
                } else {
                    createDefaultIndex(wwwDir, project)
                }
            }
            is Project.ContentSource.Remote -> {
                // For remote, create a redirect page
                createRemoteIndex(wwwDir, project)
            }
        }
    }

    private fun copySourceFiles(context: Context, sourcePath: String, destDir: File) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            // Try to resolve as content URI
            try {
                val uri = Uri.parse(sourcePath)
                if (uri.scheme == "content") {
                    copyFromContentUri(context, uri, destDir)
                    return
                }
            } catch (e: Exception) {
                // Ignore
            }
            createDefaultIndex(destDir, Project.createDefault())
            return
        }

        if (sourceFile.isFile) {
            sourceFile.copyTo(File(destDir, sourceFile.name), overwrite = true)
        } else {
            copyDirectory(sourceFile, destDir)
        }
    }

    private fun copyFromContentUri(context: Context, uri: Uri, destDir: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            if (input != null) {
                val destFile = File(destDir, "index.html")
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun copyDirectory(srcDir: File, destDir: File) {
        srcDir.walkTopDown().forEach { srcFile ->
            val relativePath = srcFile.path.substring(srcDir.path.length + 1)
            val destFile = File(destDir, relativePath)
            if (srcFile.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                srcFile.copyTo(destFile, overwrite = true)
            }
        }
    }

    private fun createDefaultIndex(wwwDir: File, project: Project) {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${project.appName.ifBlank { project.name }}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, ${project.primaryColor}22, ${project.primaryColor}44);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            text-align: center;
            background: white;
            padding: 40px;
            border-radius: 16px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            max-width: 400px;
        }
        h1 { color: ${project.primaryColor}; margin-bottom: 16px; }
        p { color: #666; line-height: 1.6; }
        .badge {
            display: inline-block;
            background: ${project.primaryColor};
            color: white;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>${project.appName.ifBlank { project.name }}</h1>
        <p>Your HTML-to-APK app is ready! Replace this content with your own HTML, CSS, and JavaScript files.</p>
        <div class="badge">Package: ${project.packageName}</div>
        <div class="badge">Version: ${project.versionName} (${project.versionCode})</div>
    </div>
</body>
</html>
        """.trimIndent()

        File(wwwDir, "index.html").writeText(html)
    }

    private fun createRemoteIndex(wwwDir: File, project: Project) {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${project.appName.ifBlank { project.name }}</title>
    <script>
        // Redirect to remote URL
        window.location.href = "${project.contentSource.url}";
    </script>
</head>
<body>
    <p>Loading ${project.appName.ifBlank { project.name }}...</p>
    <p><a href="${project.contentSource.url}">Click here if not redirected</a></p>
</body>
</html>
        """.trimIndent()

        File(wwwDir, "index.html").writeText(html)
    }

    private fun copyIcons(context: Context, project: Project, templateDir: File) {
        val iconPath = project.iconPath.ifBlank { return }
        val iconFile = resolveIconFile(context, iconPath)
        if (iconFile == null || !iconFile.exists()) return

        val densities = mapOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )

        densities.forEach { (dirName, size) ->
            val destDir = File(templateDir, "res/$dirName")
            destDir.mkdirs()
            val destFile = File(destDir, "ic_launcher.png")
            resizeAndSaveIcon(iconFile, destFile, size)

            // Round icon
            val roundFile = File(destDir, "ic_launcher_round.png")
            resizeAndSaveIcon(iconFile, roundFile, size)
        }
    }

    private fun resolveIconFile(context: Context, path: String): File? {
        return try {
            val file = File(path)
            if (file.exists()) file else {
                val uri = Uri.parse(path)
                if (uri.scheme == "content") {
                    val tempFile = File(context.cacheDir, "temp_icon.png")
                    context.contentResolver.openInputStream(uri).use { input ->
                        input?.copyTo(FileOutputStream(tempFile))
                    }
                    tempFile
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resizeAndSaveIcon(srcFile: File, destFile: File, size: Int) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(srcFile.absolutePath, options)

            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, size, size)

            val bitmap = BitmapFactory.decodeFile(srcFile.absolutePath, options)
            val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)

            FileOutputStream(destFile).use { output ->
                resized.compress(Bitmap.CompressFormat.PNG, 100, output)
            }

            bitmap.recycle()
            resized.recycle()
        } catch (e: Exception) {
            // Fallback: copy original
            srcFile.copyTo(destFile, overwrite = true)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun copySplash(context: Context, project: Project, templateDir: File) {
        val splashPath = project.splashPath.ifBlank { return }
        val splashFile = resolveIconFile(context, splashPath)
        if (splashFile == null || !splashFile.exists()) return

        val densities = listOf("drawable-mdpi", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi")
        densities.forEach { dirName ->
            val destDir = File(templateDir, "res/$dirName")
            destDir.mkdirs()
            val destFile = File(destDir, "splash.png")
            splashFile.copyTo(destFile, overwrite = true)
        }
    }
}