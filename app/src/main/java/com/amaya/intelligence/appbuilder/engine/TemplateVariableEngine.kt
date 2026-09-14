package com.amaya.intelligence.appbuilder.engine

/**
 * Replaces template placeholders in content and file paths.
 */
object TemplateVariableEngine {

    /**
     * Builds standard map of variable replacements from ProjectConfig.
     */
    fun buildReplacementMap(config: ProjectConfig): Map<String, String> {
        val packagePath = config.packageName.replace('.', '/')
        val appName = if (config.appName.isNotBlank()) config.appName else config.projectName

        return mapOf(
            "{{PROJECT_NAME}}" to config.projectName,
            "{{APP_NAME}}" to appName,
            "{{PACKAGE_NAME}}" to config.packageName,
            "{{PACKAGE_PATH}}" to packagePath,
            "{{ACTIVITY_NAME}}" to config.activityName,
            "{{MIN_SDK}}" to config.minSdk.toString(),
            "{{TARGET_SDK}}" to config.targetSdk.toString(),
            "{{COMPILE_SDK}}" to config.compileSdk.toString(),
            "{{VERSION_NAME}}" to config.versionName,
            "{{VERSION_CODE}}" to config.versionCode.toString()
        )
    }

    /**
     * Substitutes all template variables in a text string.
     */
    fun process(text: String, config: ProjectConfig): String {
        return process(text, buildReplacementMap(config))
    }

    /**
     * Substitutes all variables in a text string using a provided replacements map.
     */
    fun process(text: String, replacements: Map<String, String>): String {
        var result = text
        for ((token, value) in replacements) {
            result = result.replace(token, value)
        }
        return result
    }

    /**
     * Resolves a file path that might contain template placeholders like {{PACKAGE_PATH}} or {{ACTIVITY_NAME}}.
     */
    fun resolvePath(rawPath: String, config: ProjectConfig): String {
        val processed = process(rawPath, config)
        return processed.replace('\\', '/')
    }
}
