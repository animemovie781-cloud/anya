package com.amaya.intelligence.appbuilder.engine

import java.io.File

/**
 * Validation result wrapper.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val errorMessage: String? get() = (this as? Invalid)?.message
}

/**
 * Validates project configurations and generation environments.
 */
object ProjectValidator {

    private val FORBIDDEN_FILE_CHARS = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')
    private val IDENTIFIER_REGEX = Regex("^[a-zA-Z_$][a-zA-Z0-9_$]*$")

    /**
     * Validates project name.
     */
    fun validateProjectName(projectName: String): ValidationResult {
        val trimmed = projectName.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Project name cannot be empty")
        }
        if (trimmed.any { it in FORBIDDEN_FILE_CHARS }) {
            return ValidationResult.Invalid("Project name contains illegal characters: / \\ : * ? \" < > |")
        }
        if (trimmed == "." || trimmed == "..") {
            return ValidationResult.Invalid("Project name cannot be '.' or '..'")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates package name.
     */
    fun validatePackageName(packageName: String): ValidationResult {
        return PackagePathGenerator.validate(packageName)
    }

    /**
     * Validates activity name.
     */
    fun validateActivityName(activityName: String): ValidationResult {
        val trimmed = activityName.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Activity name cannot be empty")
        }
        val cleanName = if (trimmed.startsWith(".")) trimmed.substring(1) else trimmed
        if (!IDENTIFIER_REGEX.matches(cleanName)) {
            return ValidationResult.Invalid("Activity name '$activityName' is not a valid identifier")
        }
        if (!cleanName[0].isUpperCase()) {
            return ValidationResult.Invalid("Activity name should start with an uppercase letter (e.g. MainActivity)")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates SDK versions.
     */
    fun validateSdkVersions(minSdk: Int, targetSdk: Int, compileSdk: Int): ValidationResult {
        if (minSdk < 1) {
            return ValidationResult.Invalid("minSdk must be at least 1")
        }
        if (targetSdk < minSdk) {
            return ValidationResult.Invalid("targetSdk ($targetSdk) cannot be lower than minSdk ($minSdk)")
        }
        if (compileSdk < targetSdk) {
            return ValidationResult.Invalid("compileSdk ($compileSdk) cannot be lower than targetSdk ($targetSdk)")
        }
        return ValidationResult.Valid
    }

    /**
     * Checks if a project directory with the given name already exists in target parent directory.
     */
    fun checkDuplicateProject(parentDir: File, projectName: String): Boolean {
        val projectDir = File(parentDir, projectName.trim())
        return projectDir.exists() && (projectDir.list()?.isNotEmpty() == true)
    }

    /**
     * Validates the complete ProjectConfig.
     */
    fun validateConfig(config: ProjectConfig): ValidationResult {
        val nameRes = validateProjectName(config.projectName)
        if (!nameRes.isValid) return nameRes

        val pkgRes = validatePackageName(config.packageName)
        if (!pkgRes.isValid) return pkgRes

        val actRes = validateActivityName(config.activityName)
        if (!actRes.isValid) return actRes

        val sdkRes = validateSdkVersions(config.minSdk, config.targetSdk, config.compileSdk)
        if (!sdkRes.isValid) return sdkRes

        val lang = config.normalizedLanguage()
        val ui = config.normalizedUiType()
        if (lang != "kotlin" && lang != "java") {
            return ValidationResult.Invalid("Unsupported language '$lang'. Supported: Kotlin, Java")
        }
        if (ui != "xml" && ui != "compose") {
            return ValidationResult.Invalid("Unsupported UI type '$ui'. Supported: XML, Compose")
        }
        if (lang == "java" && ui == "compose") {
            return ValidationResult.Invalid("Jetpack Compose is not supported with Java. Please select Kotlin.")
        }

        return ValidationResult.Valid
    }
}
