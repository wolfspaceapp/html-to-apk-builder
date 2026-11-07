package com.htmltoapk.builder.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ContentSource {
    @Serializable
    data class Local(
        val path: String = ""
    ) : ContentSource()

    @Serializable
    data class Remote(
        val url: String = ""
    ) : ContentSource()

    companion object {
        fun local(path: String) = Local(path)
        fun remote(url: String) = Remote(url)

        fun fromJson(json: Json, type: String, path: String, url: String): ContentSource {
            return when (type) {
                "local" -> Local(path)
                "remote" -> Remote(url)
                else -> Local(path)
            }
        }
    }
}