package com.amaya.intelligence.appbuilder.engine

import java.io.File
import java.io.IOException

/**
 * Handles the physical creation, variable processing, and writing of files on disk.
 */
object FileGenerator {

    /**
     * Validates that target relative path does not escape project root.
     */
    fun validatePath(projectDir: File, relativePath: String): File {
        val cleanRel = relativePath.trim().replace('\\', '/')
        if (cleanRel.split('/').any { it == ".." }) {
            throw SecurityException("Path traversal attempt detected in path: $relativePath")
        }
        val file = File(projectDir, cleanRel)
        val canonicalProject = projectDir.canonicalFile
        val canonicalTarget = file.canonicalFile

        if (!canonicalTarget.path.startsWith(canonicalProject.path)) {
            throw SecurityException("Path escapes project directory: $relativePath")
        }
        return canonicalTarget
    }

    /**
     * Writes a single template file into project directory after processing variables.
     */
    fun writeFile(
        projectDir: File,
        templateFile: TemplateFile,
        config: ProjectConfig,
        existingWrittenPaths: MutableSet<String>
    ): File {
        val resolvedRelPath = TemplateVariableEngine.resolvePath(templateFile.relativePath, config)

        if (!existingWrittenPaths.add(resolvedRelPath.lowercase())) {
            throw IllegalStateException("Duplicate file detected in generation: $resolvedRelPath")
        }

        val targetFile = validatePath(projectDir, resolvedRelPath)

        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            val made = parent.mkdirs()
            if (!made && !parent.exists()) {
                throw IOException("Failed to create parent directory for file: ${targetFile.path}")
            }
        }

        val processedContent = TemplateVariableEngine.process(templateFile.content, config)

        targetFile.writeText(processedContent, Charsets.UTF_8)
        return targetFile
    }

    /**
     * Writes a list of template files into the project directory.
     */
    fun writeFiles(
        projectDir: File,
        files: List<TemplateFile>,
        config: ProjectConfig
    ): List<File> {
        val writtenPaths = mutableSetOf<String>()
        val createdFiles = mutableListOf<File>()

        for (tf in files) {
            val file = writeFile(projectDir, tf, config, writtenPaths)
            createdFiles.add(file)
        }

        return createdFiles
    }
}
