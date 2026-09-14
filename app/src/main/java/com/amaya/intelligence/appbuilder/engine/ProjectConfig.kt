package com.amaya.intelligence.appbuilder.engine

/**
 * Model representing the inputs required to generate a new Android project.
 */
data class ProjectConfig(
    val projectName: String,
    val packageName: String,
    val language: String = "kotlin", // "kotlin" or "java"
    val uiType: String = "xml",       // "xml" or "compose"
    val minSdk: Int = 24,
    val targetSdk: Int = 34,
    val compileSdk: Int = 34,
    val activityName: String = "MainActivity",
    val templateId: String = "",
    val appName: String = projectName,
    val versionName: String = "1.0",
    val versionCode: Int = 1
) {
    fun normalizedLanguage(): String = language.trim().lowercase()

    fun normalizedUiType(): String = uiType.trim().lowercase()

    fun resolveTemplateId(): String {
        if (templateId.isNotBlank()) return templateId
        val lang = normalizedLanguage()
        val ui = normalizedUiType()
        return when {
            lang == "kotlin" && ui == "compose" -> "kotlin_compose_empty"
            lang == "kotlin" && ui == "xml" -> "kotlin_xml_empty"
            lang == "java" && ui == "xml" -> "java_xml_empty"
            lang == "java" -> "java_xml_empty"
            else -> "kotlin_xml_empty"
        }
    }
}
