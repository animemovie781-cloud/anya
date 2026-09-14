package com.amaya.intelligence.appbuilder.engine

/**
 * Metadata describing a project template.
 */
data class TemplateMetadata(
    val templateId: String,
    val name: String,
    val language: String,
    val uiType: String,
    val minimumSdk: Int = 24,
    val requiredFiles: List<String> = emptyList(),
    val directories: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

/**
 * Represents a starter template file with relative destination path and text content.
 */
data class TemplateFile(
    val relativePath: String,
    val content: String
)

/**
 * Result of project generation.
 */
sealed class GenerationResult {
    data class Success(
        val projectDir: java.io.File,
        val createdFiles: List<java.io.File>,
        val config: ProjectConfig
    ) : GenerationResult()

    data class DuplicateProject(
        val existingDir: java.io.File,
        val config: ProjectConfig
    ) : GenerationResult()

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : GenerationResult()
}
