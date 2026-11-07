package com.htmltoapk.builder.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.R
import com.htmltoapk.builder.data.AppDatabase
import com.htmltoapk.builder.data.KeystoreEntity
import com.htmltoapk.builder.data.ProjectEntity
import com.htmltoapk.builder.databinding.ActivityProjectEditorBinding
import com.htmltoapk.builder.model.AdMobSettings
import com.htmltoapk.builder.model.BuildConfiguration
import com.htmltoapk.builder.model.ContentSource
import com.htmltoapk.builder.model.StartAppSettings
import com.htmltoapk.builder.model.UnityAdsSettings
import com.htmltoapk.builder.viewmodel.ProjectEditorViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import java.io.File

class ProjectEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectEditorBinding
    private val viewModel: ProjectEditorViewModel by viewModels()
    private var projectId: Long = 0
    private var isEditing = false

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleFileSelection(it) }
    }

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { handleFolderSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getLongExtra("project_id", 0)
        isEditing = projectId != 0L

        setupToolbar()
        setupSpinners()
        setupClickListeners()
        setupObservers()

        if (isEditing) {
            loadProject()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditing) "Edit Project" else "New Project"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupSpinners() {
        // Build mode spinner
        val buildModes = arrayOf("debug", "release")
        binding.spinnerBuildMode.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, buildModes
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Output format spinner
        val outputFormats = arrayOf("apk", "aab")
        binding.spinnerOutputFormat.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, outputFormats
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Orientation spinner
        val orientations = arrayOf("portrait", "landscape", "sensor")
        binding.spinnerOrientation.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, orientations
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Content source spinner
        val contentSources = arrayOf("Local", "Remote")
        binding.spinnerContentSource.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, contentSources
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.spinnerContentSource.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isLocal = position == 0
                binding.layoutLocalSource.visibility = if (isLocal) View.VISIBLE else View.GONE
                binding.layoutRemoteSource.visibility = if (isLocal) View.GONE else View.VISIBLE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectSourcePath.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnSelectIcon.setOnClickListener {
            filePickerLauncher.launch("image/*")
        }

        binding.btnSelectSplash.setOnClickListener {
            filePickerLauncher.launch("image/*")
        }

        binding.btnSelectKeystore.setOnClickListener {
            val intent = Intent(this, KeystoreManagerActivity::class.java)
            startActivity(intent)
        }

        binding.btnSave.setOnClickListener {
            saveProject()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.keystores.observe(this) { keystores ->
            val names = keystores.map { it.name }.toTypedArray()
            binding.spinnerKeystore.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, names
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
    }

    private fun loadProject() {
        lifecycleScope.launch {
            val projectEntity = HtmlToApkApplication.getInstance().getDatabase().projectDao().getProjectById(projectId).first()
            projectEntity?.let { populateFields(it) }
        }
    }

    private fun populateFields(project: ProjectEntity) {
        binding.etProjectName.setText(project.name)
        binding.etPackageName.setText(project.packageName)
        binding.etVersionCode.setText(project.versionCode.toString())
        binding.etVersionName.setText(project.versionName)
        binding.etAppName.setText(project.appName)
        binding.etMinSdk.setText(project.minSdk.toString())
        binding.etTargetSdk.setText(project.targetSdk.toString())
        binding.etPrimaryColor.setText(project.primaryColor)
        binding.etDeepLinkScheme.setText(project.deepLinkScheme)

        binding.switchAutoIncrementVersion.isChecked = project.autoIncrementVersionCode
        binding.switchShowActionBar.isChecked = project.showActionBar
        binding.switchShowHeader.isChecked = project.showHeader
        binding.switchShowWatermark.isChecked = project.showWatermark
        binding.switchShowCameraCutout.isChecked = project.showCameraCutout
        binding.switchKeepScreenOn.isChecked = project.keepScreenOn
        binding.switchDesktopMode.isChecked = project.desktopMode

        binding.etSourcePath.setText(project.sourcePath)
        binding.etSourceUrl.setText(project.sourceUrl)
        binding.etIconPath.setText(project.iconPath)
        binding.etSplashPath.setText(project.splashPath)

        // Set spinners
        setSpinnerSelection(binding.spinnerBuildMode, project.buildMode)
        setSpinnerSelection(binding.spinnerOutputFormat, project.outputFormat)
        setSpinnerSelection(binding.spinnerOrientation, project.orientation)
        setSpinnerSelection(binding.spinnerContentSource, project.contentSourceType)

        // Monetization fields - using JSON stored in entity
        val json = kotlinx.serialization.json.Json { prettyPrint = true }

        // AdMob
        try {
            val admob = json.decodeFromString<com.htmltoapk.builder.model.AdMobSettings>(project.admobJson)
            binding.switchAdMobEnabled.isChecked = admob.enabled
            binding.etAdMobAppId.setText(admob.appId)
            binding.etAdMobBannerId.setText(admob.bannerUnitId)
            binding.etAdMobInterstitialId.setText(admob.interstitialUnitId)
            binding.etAdMobRewardedId.setText(admob.rewardedUnitId)
            binding.switchAdMobTestAds.isChecked = admob.useTestAds
        } catch (e: Exception) {
            // Use defaults
        }

        // Unity Ads
        try {
            val unityAds = json.decodeFromString<UnityAdsSettings>(project.unityAdsJson)
            binding.switchUnityAdsEnabled.isChecked = unityAds.enabled
            binding.etUnityAdsGameId.setText(unityAds.gameId)
            binding.etUnityAdsBannerPlacement.setText(unityAds.bannerPlacementId)
            binding.etUnityAdsInterstitialPlacement.setText(unityAds.interstitialPlacementId)
            binding.etUnityAdsRewardedPlacement.setText(unityAds.rewardedPlacementId)
            binding.switchUnityAdsTestMode.isChecked = unityAds.testMode
        } catch (e: Exception) {
            // Use defaults
        }

        // StartApp
        try {
            val startApp = json.decodeFromString<StartAppSettings>(project.startAppJson)
            binding.switchStartAppEnabled.isChecked = startApp.enabled
            binding.etStartAppAppId.setText(startApp.appId)
            binding.etStartAppDevId.setText(startApp.devId)
            binding.switchStartAppTestMode.isChecked = startApp.testMode
        } catch (e: Exception) {
            // Use defaults
        }

        // Keystore
        if (project.keystoreId != null) {
            viewModel.keystores.observe(this) { keystores ->
                val index = keystores.indexOfFirst { it.id == project.keystoreId }
                if (index >= 0) {
                    binding.spinnerKeystore.setSelection(index)
                }
            }
        }
    }

    private fun setSpinnerSelection(spinner: android.widget.Spinner, value: String) {
        val adapter = spinner.adapter as? ArrayAdapter<*>
        adapter?.let {
            val position = it.getPosition(value)
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }
    }

    private fun handleFileSelection(uri: Uri) {
        val path = getPathFromUri(uri)
        // Determine which field to populate based on context
        // For simplicity, we'll use a tag or dialog to choose
        showFileTargetDialog(path)
    }

    private fun handleFolderSelection(uri: Uri) {
        val path = getPathFromUri(uri)
        binding.etSourcePath.setText(path)
    }

    private fun showFileTargetDialog(path: String) {
        val options = arrayOf("Source Path", "Icon", "Splash Screen")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select target")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> binding.etSourcePath.setText(path)
                    1 -> binding.etIconPath.setText(path)
                    2 -> binding.etSplashPath.setText(path)
                }
            }
            .show()
    }

    private fun getPathFromUri(uri: Uri): String {
        // Simplified - in production use DocumentFile or MediaStore
        return uri.toString()
    }

    private fun saveProject() {
        val name = binding.etProjectName.text.toString().trim()
        val packageName = binding.etPackageName.text.toString().trim()
        val versionCodeStr = binding.etVersionCode.text.toString().trim()
        val versionName = binding.etVersionName.text.toString().trim()
        val appName = binding.etAppName.text.toString().trim()

        if (TextUtils.isEmpty(name)) {
            binding.etProjectName.error = "Project name is required"
            return
        }
        if (TextUtils.isEmpty(packageName)) {
            binding.etPackageName.error = "Package name is required"
            return
        }
        if (TextUtils.isEmpty(versionCodeStr)) {
            binding.etVersionCode.error = "Version code is required"
            return
        }

        val versionCode = versionCodeStr.toIntOrNull() ?: 1

        val json = Json { prettyPrint = true }

        // Build AdMob settings
        val admob = AdMobSettings(
            enabled = binding.switchAdMobEnabled.isChecked,
            appId = binding.etAdMobAppId.text.toString(),
            bannerEnabled = binding.etAdMobBannerId.text.toString().isNotBlank(),
            bannerUnitId = binding.etAdMobBannerId.text.toString(),
            interstitialEnabled = binding.etAdMobInterstitialId.text.toString().isNotBlank(),
            interstitialUnitId = binding.etAdMobInterstitialId.text.toString(),
            rewardedEnabled = binding.etAdMobRewardedId.text.toString().isNotBlank(),
            rewardedUnitId = binding.etAdMobRewardedId.text.toString(),
            useTestAds = binding.switchAdMobTestAds.isChecked
        )

        // Build Unity Ads settings
        val unityAds = UnityAdsSettings(
            enabled = binding.switchUnityAdsEnabled.isChecked,
            gameId = binding.etUnityAdsGameId.text.toString(),
            testMode = binding.switchUnityAdsTestMode.isChecked,
            bannerEnabled = binding.etUnityAdsBannerPlacement.text.toString().isNotBlank(),
            bannerPlacementId = binding.etUnityAdsBannerPlacement.text.toString(),
            interstitialEnabled = binding.etUnityAdsInterstitialPlacement.text.toString().isNotBlank(),
            interstitialPlacementId = binding.etUnityAdsInterstitialPlacement.text.toString(),
            rewardedEnabled = binding.etUnityAdsRewardedPlacement.text.toString().isNotBlank(),
            rewardedPlacementId = binding.etUnityAdsRewardedPlacement.text.toString()
        )

        // Build StartApp settings
        val startApp = StartAppSettings(
            enabled = binding.switchStartAppEnabled.isChecked,
            appId = binding.etStartAppAppId.text.toString(),
            devId = binding.etStartAppDevId.text.toString(),
            testMode = binding.switchStartAppTestMode.isChecked
        )

        val project = ProjectEntity(
            id = if (isEditing) projectId else 0,
            name = name,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            appName = appName,
            contentSourceType = binding.spinnerContentSource.selectedItem?.toString()?.lowercase() ?: "local",
            contentSourcePath = binding.etSourcePath.text.toString(),
            contentSourceUrl = binding.etSourceUrl.text.toString(),
            sourcePath = binding.etSourcePath.text.toString(),
            sourceUrl = binding.etSourceUrl.text.toString(),
            iconPath = binding.etIconPath.text.toString(),
            splashPath = binding.etSplashPath.text.toString(),
            minSdk = binding.etMinSdk.text.toString().toIntOrNull() ?: 24,
            targetSdk = binding.etTargetSdk.text.toString().toIntOrNull() ?: 34,
            orientation = binding.spinnerOrientation.selectedItem?.toString() ?: "portrait",
            buildMode = binding.spinnerBuildMode.selectedItem?.toString() ?: "debug",
            outputFormat = binding.spinnerOutputFormat.selectedItem?.toString() ?: "apk",
            primaryColor = binding.etPrimaryColor.text.toString(),
            showActionBar = binding.switchShowActionBar.isChecked,
            showHeader = binding.switchShowHeader.isChecked,
            showWatermark = binding.switchShowWatermark.isChecked,
            showCameraCutout = binding.switchShowCameraCutout.isChecked,
            keepScreenOn = binding.switchKeepScreenOn.isChecked,
            desktopMode = binding.switchDesktopMode.isChecked,
            deepLinkScheme = binding.etDeepLinkScheme.text.toString(),
            autoIncrementVersionCode = binding.switchAutoIncrementVersion.isChecked,
            admobJson = json.encodeToString(AdMobSettings.serializer(), admob),
            unityAdsJson = json.encodeToString(UnityAdsSettings.serializer(), unityAds),
            startAppJson = json.encodeToString(StartAppSettings.serializer(), startApp),
            billingJson = "{}",
            umpJson = "{}",
            keystoreId = getSelectedKeystoreId(),
            updatedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            if (isEditing) {
                HtmlToApkApplication.getInstance().getDatabase().projectDao().update(project)
            } else {
                HtmlToApkApplication.getInstance().getDatabase().projectDao().insert(project)
            }

            Toast.makeText(this@ProjectEditorActivity, if (isEditing) "Project updated" else "Project created", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getSelectedKeystoreId(): Long? {
        val position = binding.spinnerKeystore.selectedItemPosition
        if (position >= 0) {
            val keystores = viewModel.keystores.value
            return keystores?.getOrNull(position)?.id
        }
        return null
    }
}