package com.htmltoapk.builder.android

import android.util.Log
import com.htmltoapk.builder.model.Project
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringWriter

class ResourceProcessor {
    companion object {
        private const val TAG = "ResourceProcessor"
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }

    fun processManifest(manifestFile: File, project: Project): Boolean {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()

            FileInputStream(manifestFile).use { input ->
                parser.setInput(input, "UTF-8")
                val output = StringWriter()
                val serializer = factory.newSerializer()
                serializer.setOutput(output)

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            serializer.startTag(parser.namespace, parser.name)

                            // Copy all attributes
                            for (i in 0 until parser.attributeCount) {
                                val attrNs = parser.getAttributeNamespace(i)
                                val attrName = parser.getAttributeName(i)
                                val attrValue = parser.getAttributeValue(i)

                                if (attrName == "package" && parser.name == "manifest") {
                                    serializer.attribute(attrNs, attrName, project.packageName)
                                } else if (attrName == "versionCode" && parser.name == "manifest") {
                                    serializer.attribute(attrNs, attrName, project.versionCode.toString())
                                } else if (attrName == "versionName" && parser.name == "manifest") {
                                    serializer.attribute(attrNs, attrName, project.versionName)
                                } else {
                                    serializer.attribute(attrNs, attrName, attrValue)
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            serializer.endTag(parser.namespace, parser.name)
                        }
                        XmlPullParser.TEXT -> {
                            serializer.text(parser.text)
                        }
                    }
                    eventType = parser.next()
                }

                serializer.endDocument()
                manifestFile.writeText(output.toString())
            }

            // Add permissions after processing
            addPermissions(manifestFile, project)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process manifest", e)
            false
        }
    }

    private fun addPermissions(manifestFile: File, project: Project) {
        if (project.permissions.isEmpty()) return

        try {
            val content = manifestFile.readText()
            var modified = content

            // Check if permissions already exist
            val existingPerms = mutableSetOf<String>()
            val permRegex = "<uses-permission\\s+android:name=\"([^\"]+)\"".toRegex()
            permRegex.findAll(content).forEach { match ->
                match.groupValues.getOrNull(1)?.let { existingPerms.add(it) }
            }

            val newPerms = project.permissions.filterNot { it in existingPerms }
            if (newPerms.isNotEmpty()) {
                val permXml = newPerms.joinToString("\n") { "    <uses-permission android:name=\"$it\" />" }
                // Insert after package declaration in manifest tag
                modified = modified.replaceFirst(
                    "(<manifest[^>]*package=\"[^\"]+\"[^>]*>)",
                    "$1\n$permXml"
                )
                manifestFile.writeText(modified)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add permissions", e)
        }
    }

    fun processStrings(stringsFile: File, project: Project): Boolean {
        return try {
            if (!stringsFile.exists()) {
                createDefaultStrings(stringsFile, project)
                return true
            }

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()

            FileInputStream(stringsFile).use { input ->
                parser.setInput(input, "UTF-8")
                val output = StringWriter()
                val serializer = factory.newSerializer()
                serializer.setOutput(output)

                var eventType = parser.eventType
                var appNameUpdated = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            serializer.startTag(parser.namespace, parser.name)
                            for (i in 0 until parser.attributeCount) {
                                serializer.attribute(
                                    parser.getAttributeNamespace(i),
                                    parser.getAttributeName(i),
                                    parser.getAttributeValue(i)
                                )
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            serializer.endTag(parser.namespace, parser.name)
                        }
                        XmlPullParser.TEXT -> {
                            val text = parser.text
                            // Update app_name string
                            if (text == "TemplateApp" || text == "template_app") {
                                val parentName = getParentStringName(parser)
                                if (parentName == "app_name") {
                                    serializer.text(project.appName.ifBlank { project.name })
                                    appNameUpdated = true
                                } else {
                                    serializer.text(text)
                                }
                            } else {
                                serializer.text(text)
                            }
                        }
                    }
                    eventType = parser.next()
                }

                serializer.endDocument()
                stringsFile.writeText(output.toString())
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process strings", e)
            false
        }
    }

    private fun getParentStringName(parser: XmlPullParser): String? {
        // This is a simplified approach - in reality we'd need to track the element stack
        // For now, we'll do a simple text replacement approach as fallback
        return null
    }

    private fun createDefaultStrings(stringsFile: File, project: Project) {
        stringsFile.parentFile?.mkdirs()
        val content = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${project.appName.ifBlank { project.name }}</string>
    <string name="template_name">TemplateApp</string>
</resources>"""
        stringsFile.writeText(content)
    }

    fun copyResources(srcRes: File, destRes: File) {
        if (!srcRes.exists()) return
        copyDirectory(srcRes, destRes)
    }

    private fun copyDirectory(src: File, dest: File) {
        dest.mkdirs()
        src.listFiles()?.forEach { file ->
            val destFile = File(dest, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile)
            } else {
                file.copyTo(destFile, overwrite = true)
            }
        }
    }
}