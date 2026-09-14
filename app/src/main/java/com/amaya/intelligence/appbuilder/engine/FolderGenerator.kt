package com.amaya.intelligence.appbuilder.engine

import java.io.File

/**
 * Creates directories required for an Android Studio project structure.
 */
object FolderGenerator {

    /**
     * Creates all directories specified in directories list or default required directories.
     */
    fun createProjectDirectories(
        projectDir: File,
        config: ProjectConfig,
        customDirectories: List<String> = emptyList()
    ): List<File> {
        val created = mutableListOf<File>()

        fun ensureDir(relativePath: String): File {
            val resolved = TemplateVariableEngine.resolvePath(relativePath, config)
            val dir = File(projectDir, resolved)
            if (!dir.exists()) {
                val ok = dir.mkdirs()
                if (!ok && !dir.exists()) {
                    throw IllegalStateException("Failed to create directory: ${dir.absolutePath}")
                }
            }
            created.add(dir)
            return dir
        }

        // Standard root project folders
        ensureDir("app")
        ensureDir("app/src/main")
        ensureDir("gradle/wrapper")

        // Source folder based on language
        val isKotlin = config.normalizedLanguage() == "kotlin"
        val srcLang = if (isKotlin) "kotlin" else "java"
        val pkgPath = PackagePathGenerator.toPackagePath(config.packageName)
        ensureDir("app/src/main/$srcLang/$pkgPath")

        // Resource folders
        ensureDir("app/src/main/res")
        ensureDir("app/src/main/res/drawable")
        ensureDir("app/src/main/res/mipmap")
        ensureDir("app/src/main/res/values")

        // If XML UI type, ensure layout folder
        if (config.normalizedUiType() == "xml") {
            ensureDir("app/src/main/res/layout")
        }

        // Custom template directories if provided
        for (relPath in customDirectories) {
            if (relPath.isNotBlank()) {
                ensureDir(relPath)
            }
        }

        return created
    }
}
