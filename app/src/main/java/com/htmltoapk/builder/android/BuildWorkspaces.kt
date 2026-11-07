package com.htmltoapk.builder.android

import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class BuildWorkspaces(private val baseDir: File) {
    companion object {
        private const val TAG = "BuildWorkspaces"
        @Volatile
        private var INSTANCE: BuildWorkspaces? = null

        fun getInstance(baseDir: File? = null): BuildWorkspaces {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BuildWorkspaces(
                    baseDir ?: File(System.getProperty("java.io.tmpdir"), "html-to-apk-builds")
                ).also { INSTANCE = it }
            }
        }
    }

    private val activeWorkspaces = mutableSetOf<File>()
    private val counter = AtomicInteger(0)

    init {
        baseDir.mkdirs()
    }

    fun create(projectName: String): File {
        val sanitizedName = projectName.replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
        val workspaceName = "${sanitizedName}_${counter.incrementAndGet()}_${System.currentTimeMillis()}"
        val workspace = File(baseDir, workspaceName)
        workspace.mkdirs()
        activeWorkspaces.add(workspace)
        Log.d(TAG, "Created workspace: ${workspace.absolutePath}")
        return workspace
    }

    fun release(workspace: File) {
        activeWorkspaces.remove(workspace)
    }

    fun clean(workspace: File) {
        if (workspace.exists()) {
            deleteRecursively(workspace)
        }
        activeWorkspaces.remove(workspace)
        Log.d(TAG, "Cleaned workspace: ${workspace.absolutePath}")
    }

    fun cleanAll() {
        activeWorkspaces.forEach { clean(it) }
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}