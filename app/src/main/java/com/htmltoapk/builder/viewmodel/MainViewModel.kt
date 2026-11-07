package com.htmltoapk.builder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.data.AppDatabase
import com.htmltoapk.builder.data.ProjectEntity
import com.htmltoapk.builder.model.BuildConfiguration
import com.htmltoapk.builder.signing.ApkSigner
import com.htmltoapk.builder.signing.SigningKey
import com.htmltoapk.builder.signing.SigningKeyResolver
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = HtmlToApkApplication.getInstance().getDatabase()
    private val apkSigner = ApkSigner()
    private val signingKeyResolver = SigningKeyResolver()

    val projects = database.projectDao().getAllProjects().asLiveData()

    sealed interface BuildState {
        data class Building(val message: String) : BuildState
        data class Success(val outputFile: File) : BuildState
        data class Error(val message: String) : BuildState
    }

    val buildState = MutableLiveData<BuildState>()

    fun insertProject(project: ProjectEntity) {
        viewModelScope.launch {
            database.projectDao().insert(project)
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            database.projectDao().update(project)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            database.projectDao().deleteById(projectId)
        }
    }

    fun buildProject(projectId: Long) {
        viewModelScope.launch {
            buildState.postValue(BuildState.Building("Preparing build..."))

            val projectEntity = database.projectDao().getProjectById(projectId).first()
            if (projectEntity == null) {
                buildState.postValue(BuildState.Error("Project not found"))
                return@launch
            }

            val project = projectEntity.toProject()

            try {
                buildState.postValue(BuildState.Building("Resolving signing key..."))

                val signingKey: SigningKey? = if (project.keystoreId != null) {
                    signingKeyResolver.resolveKeystore(project.keystoreId!!)
                } else {
                    signingKeyResolver.getDefaultKeystore()
                }

                if (signingKey == null) {
                    buildState.postValue(BuildState.Error("No signing key configured. Please add a keystore in settings."))
                    return@launch
                }

                buildState.postValue(BuildState.Building("Creating build workspace..."))

                val outputDir = File(getApplication<Application>().cacheDir, "builds/${project.name}_${System.currentTimeMillis()}")
                outputDir.mkdirs()

                val outputFile = File(outputDir, "${project.name}_v${project.versionName}.${project.outputFormat}")

                // Here we would call the actual build pipeline
                // For now, simulate build process
                buildState.postValue(BuildState.Building("Building ${project.outputFormat.toUpperCase()}..."))

                // Simulate build steps
                Thread.sleep(1000) // Template extraction
                buildState.postValue(BuildState.Building("Extracting template..."))
                Thread.sleep(1000) // Asset writing
                buildState.postValue(BuildState.Building("Writing project assets..."))
                Thread.sleep(1000) // DEX repackaging
                buildState.postValue(BuildState.Building("Repackaging DEX..."))
                Thread.sleep(1000) // Resource processing
                buildState.postValue(BuildState.Building("Processing resources..."))
                Thread.sleep(1000) // Signing
                buildState.postValue(BuildState.Building("Signing APK..."))

                // Create a minimal output file for demonstration
                outputFile.createNewFile()

                buildState.postValue(BuildState.Success(outputFile))

                // Record build history
                val history = com.htmltoapk.builder.data.BuildHistoryEntity(
                    projectId = projectId,
                    outputPath = outputFile.absolutePath,
                    outputFormat = project.outputFormat,
                    success = true,
                    buildDurationMs = 5000,
                    fileSizeBytes = outputFile.length()
                )
                database.buildHistoryDao().insert(history)

            } catch (e: Exception) {
                buildState.postValue(BuildState.Error("Build failed: ${e.message}"))

                val history = com.htmltoapk.builder.data.BuildHistoryEntity(
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
}