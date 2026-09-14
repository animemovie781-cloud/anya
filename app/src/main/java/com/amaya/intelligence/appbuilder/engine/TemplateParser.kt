package com.amaya.intelligence.appbuilder.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * Parses template metadata from JSON.
 */
object TemplateParser {

    fun parse(jsonString: String): TemplateMetadata {
        val root = JSONObject(jsonString)
        val templateId = root.optString("templateId", "")
        val name = root.optString("name", "")
        val language = root.optString("language", "kotlin")
        val uiType = root.optString("uiType", "xml")
        val minimumSdk = root.optInt("minimumSdk", 24)

        val requiredFiles = root.optJSONArray("requiredFiles")?.toStringList() ?: emptyList()
        val directories = root.optJSONArray("directories")?.toStringList() ?: emptyList()
        val dependencies = root.optJSONArray("dependencies")?.toStringList() ?: emptyList()

        return TemplateMetadata(
            templateId = templateId,
            name = name,
            language = language,
            uiType = uiType,
            minimumSdk = minimumSdk,
            requiredFiles = requiredFiles,
            directories = directories,
            dependencies = dependencies
        )
    }

    fun parse(inputStream: InputStream): TemplateMetadata {
        val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parse(json)
    }

    private fun JSONArray.toStringList(): List<String> {
        val list = ArrayList<String>(length())
        for (i in 0 until length()) {
            list.add(optString(i))
        }
        return list
    }
}
