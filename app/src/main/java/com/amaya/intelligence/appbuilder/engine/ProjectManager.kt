package com.amaya.intelligence.appbuilder.engine

import android.content.Context
import android.os.Environment
import com.amaya.intelligence.ui.activities.appbuilder.AppProject
import java.io.File

/**
 * High-level manager orchestrating project creation, validation, and listing.
 */
class ProjectManager(
    private val templateManager: TemplateManager = TemplateManager()
) {

    /**
     * Determines the root directory where user projects are stored.
     */
    fun getProjectsRoot(context: Context): File {
        // Prefer app-specific external files dir (always accessible without MANAGE_EXTERNAL_STORAGE)
        val extDir = context.getExternalFilesDir("projects")
        if (extDir != null && (extDir.exists() || extDir.mkdirs())) {
            return extDir
        }
        // Fallback to internal storage
        val internalDir = File(context.filesDir, "projects")
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        return internalDir
    }

    /**
     * Executes the full project generation pipeline.
     */
    fun generateProject(
        config: ProjectConfig,
        parentDir: File,
        allowOverwrite: Boolean = false
    ): GenerationResult {
        // 1. Validation
        val validation = ProjectValidator.validateConfig(config)
        if (!validation.isValid) {
            return GenerationResult.Failure(validation.errorMessage ?: "Invalid configuration")
        }

        val projectDir = File(parentDir, config.projectName.trim())

        // 2. Safety / Existing project check
        if (projectDir.exists() && projectDir.list()?.isNotEmpty() == true && !allowOverwrite) {
            return GenerationResult.DuplicateProject(projectDir, config)
        }

        return try {
            if (!projectDir.exists()) {
                val created = projectDir.mkdirs()
                if (!created && !projectDir.exists()) {
                    return GenerationResult.Failure("Failed to create project directory: ${projectDir.path}")
                }
            }

            // 3. Resolve Template
            val template = templateManager.resolveTemplate(config.language, config.uiType)

            // 4. Create Directory Hierarchy
            FolderGenerator.createProjectDirectories(projectDir, config, template.directories)

            // 5. Gather all files to generate
            val filesToGenerate = mutableListOf<TemplateFile>()

            // Build files
            filesToGenerate.add(GradleConfigGenerator.generateRootSettings(config))
            filesToGenerate.add(GradleConfigGenerator.generateRootBuild(config))
            filesToGenerate.add(GradleConfigGenerator.generateGradleProperties())
            filesToGenerate.add(GradleConfigGenerator.generateGradleWrapperProperties())
            filesToGenerate.add(GradleConfigGenerator.generateAppBuild(config))

            // Manifest
            filesToGenerate.add(ManifestGenerator.generate(config))

            // Starter files from template
            val starterFiles = templateManager.getStarterFiles(config)
            filesToGenerate.addAll(starterFiles)

            // 6. Write Files
            val createdFiles = FileGenerator.writeFiles(projectDir, filesToGenerate, config)

            // 7. Validate Project Generation
            val manifestFile = File(projectDir, "app/src/main/AndroidManifest.xml")
            if (!manifestFile.exists()) {
                return GenerationResult.Failure("Generation validation failed: AndroidManifest.xml is missing")
            }

            GenerationResult.Success(projectDir, createdFiles, config)
        } catch (e: Exception) {
            GenerationResult.Failure("Failed to generate project: ${e.message}", e)
        }
    }

    /**
     * Lists existing projects from the projects folder.
     */
    fun listProjects(parentDir: File): List<AppProject> {
        if (!parentDir.exists() || !parentDir.isDirectory) return emptyList()

        val subdirs = parentDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        val projects = mutableListOf<AppProject>()

        for (dir in subdirs) {
            val manifest = File(dir, "app/src/main/AndroidManifest.xml")
            if (manifest.exists()) {
                val projectName = dir.name
                val appName = extractAppName(manifest) ?: projectName
                val packageName = extractPackageName(manifest) ?: "com.example.${projectName.lowercase()}"
                val id = (Math.abs(dir.name.hashCode()) % 1000).toString()

                projects.add(
                    AppProject(
                        id = id,
                        appName = appName,
                        workspaceName = projectName,
                        versionName = "1.0",
                        versionCode = "1",
                        packageName = packageName
                    )
                )
            }
        }

        return projects
    }

    private fun extractAppName(manifestFile: File): String? {
        return try {
            val text = manifestFile.readText()
            val labelMatch = Regex("""android:label="([^"]+)"""").find(text)
            labelMatch?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPackageName(manifestFile: File): String? {
        return try {
            // Check app/build.gradle.kts or app/build.gradle for namespace
            val appDir = manifestFile.parentFile?.parentFile?.parentFile
            val gradleKts = File(appDir, "build.gradle.kts")
            val gradleGroovy = File(appDir, "build.gradle")

            val gradleFile = if (gradleKts.exists()) gradleKts else if (gradleGroovy.exists()) gradleGroovy else null
            if (gradleFile != null) {
                val text = gradleFile.readText()
                val namespaceMatch = Regex("""namespace\s*=?\s*["']([^"']+)["']""").find(text)
                if (namespaceMatch != null) {
                    return namespaceMatch.groupValues[1]
                }
            }

            val text = manifestFile.readText()
            val pkgMatch = Regex("""package="([^"]+)"""").find(text)
            pkgMatch?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }
}
