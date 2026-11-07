package com.htmltoapk.builder.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.data.AppDatabase
import com.htmltoapk.builder.data.BuildHistoryEntity
import com.htmltoapk.builder.data.ProjectEntity
import com.htmltoapk.builder.model.BuildConfiguration
import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.signing.ApkSigner
import com.htmltoapk.builder.signing.SigningKey
import com.htmltoapk.builder.signing.SigningKeyResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BuildViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = HtmlToApkApplication.getInstance().getDatabase()
    private val apkSigner = ApkSigner()
    private val signingKeyResolver = SigningKeyResolver()

    private val _project = MutableLiveData<ProjectEntity?>()
    val project: LiveData<ProjectEntity?> = _project

    private val _buildHistory = MutableLiveData<List<BuildHistoryEntity>>()
    val buildHistory: LiveData<List<BuildHistoryEntity>> = _buildHistory

    sealed interface BuildState {
        data class Building(val message: String) : BuildState
        data class Success(val outputFile: File) : BuildState
        data class Error(val message: String) : BuildState
    }

    private val _buildState = MutableLiveData<BuildState>()
    val buildState: LiveData<BuildState> = _buildState

    private var lastOutputFile: File? = null

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            _project.value = database.projectDao().getProjectById(projectId).first()
        }
    }

    fun loadBuildHistory(projectId: Long) {
        database.buildHistoryDao().getHistoryForProject(projectId).asLiveData()
            .observeForever { history ->
                _buildHistory.value = history
            }
    }

    fun buildProject(projectId: Long) {
        viewModelScope.launch {
            _buildState.value = BuildState.Building("Preparing build...")

            val projectEntity = database.projectDao().getProjectById(projectId).first()
            if (projectEntity == null) {
                _buildState.value = BuildState.Error("Project not found")
                return@launch
            }

            val project = projectEntity.toProject()

            try {
                _buildState.value = BuildState.Building("Resolving signing key...")

                val signingKey: SigningKey? = if (project.keystoreId != null) {
                    signingKeyResolver.resolveKeystore(project.keystoreId!!)
                } else {
                    signingKeyResolver.getDefaultKeystore()
                }

                if (signingKey == null) {
                    _buildState.value = BuildState.Error("No signing key configured. Please add a keystore in settings.")
                    return@launch
                }

                _buildState.value = BuildState.Building("Creating build workspace...")

                val outputDir = File(getApplication<Application>().cacheDir, "builds/${project.name}_${System.currentTimeMillis()}")
                outputDir.mkdirs()

                val outputFile = File(outputDir, "${project.name}_v${project.versionName}.${project.outputFormat}")

                _buildState.value = BuildState.Building("Building ${project.outputFormat.toUpperCase()}...")

                // Simulate build steps - replace with actual build pipeline
                val success = buildApk(project, signingKey, outputFile)

                if (success) {
                    lastOutputFile = outputFile
                    _buildState.value = BuildState.Success(outputFile)

                    // Record build history
                    val history = BuildHistoryEntity(
                        projectId = projectId,
                        outputPath = outputFile.absolutePath,
                        outputFormat = project.outputFormat,
                        success = true,
                        buildDurationMs = 5000,
                        fileSizeBytes = outputFile.length()
                    )
                    database.buildHistoryDao().insert(history)
                } else {
                    _buildState.value = BuildState.Error("Build failed")
                }

            } catch (e: Exception) {
                _buildState.value = BuildState.Error("Build failed: ${e.message}")

                val history = BuildHistoryEntity(
                    projectId = projectId,
                    outputPath = "",
                    outputFormat = project.outputFormat,
                    success = false,
                    errorMessage = e.message,
                    buildDurationMs = 0
                )
                database.buildHistoryDao().insert(history)
            }
        }
    }

    private suspend fun buildApk(project: Project, signingKey: SigningKey, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate build steps
                Thread.sleep(500)
                _buildState.postValue(BuildState.Building("Extracting template..."))
                Thread.sleep(500)
                _buildState.postValue(BuildState.Building("Writing project assets..."))
                Thread.sleep(500)
                _buildState.postValue(BuildState.Building("Repackaging DEX..."))
                Thread.sleep(500)
                _buildState.postValue(BuildState.Building("Processing resources..."))
                Thread.sleep(500)
                _buildState.postValue(BuildState.Building("Signing APK..."))

                // Create a minimal output file for demonstration
                outputFile.createNewFile()

                // Try to sign if it's an APK
                if (project.outputFormat == "apk") {
                    val signedFile = File(outputFile.parentFile, "${outputFile.name.stem}-signed.apk")
                    val signed = apkSigner.signApk(outputFile, signingKey, signedFile)
                    if (signed) {
                        outputFile.delete()
                        signedFile.renameTo(outputFile)
                    }
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun openOutputFile(context: Context) {
        lastOutputFile?.let { file ->
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.fromFile(file)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(intent)
        }
    }
}