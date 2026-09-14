package com.amaya.intelligence.appbuilder.engine

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TemplateEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectManager: ProjectManager
    private lateinit var parentDir: File

    @Before
    fun setUp() {
        projectManager = ProjectManager(TemplateManager())
        parentDir = tempFolder.newFolder("projects")
    }

    /**
     * TEST 1:
     * Project: Calculator
     * Package: com.example.calculator
     * Language: Kotlin
     * UI: XML
     *
     * Expected:
     * MainActivity.kt
     * activity_main.xml
     * AndroidManifest.xml
     * Gradle files
     * resources
     */
    @Test
    fun testCase1_KotlinXmlEmpty_Calculator() {
        val config = ProjectConfig(
            projectName = "Calculator",
            packageName = "com.example.calculator",
            language = "kotlin",
            uiType = "xml",
            activityName = "MainActivity"
        )

        val result = projectManager.generateProject(config, parentDir)
        assertTrue("Generation should succeed: $result", result is GenerationResult.Success)

        val success = result as GenerationResult.Success
        val projectDir = success.projectDir

        // Verify Kotlin source file in correct package directory
        val mainActivity = File(projectDir, "app/src/main/kotlin/com/example/calculator/MainActivity.kt")
        assertTrue("MainActivity.kt must exist", mainActivity.exists())
        val mainActivityContent = mainActivity.readText()
        assertTrue(mainActivityContent.contains("package com.example.calculator"))
        assertTrue(mainActivityContent.contains("class MainActivity : AppCompatActivity()"))
        assertTrue(mainActivityContent.contains("setContentView(R.layout.activity_main)"))

        // Verify activity_main.xml
        val activityMainXml = File(projectDir, "app/src/main/res/layout/activity_main.xml")
        assertTrue("activity_main.xml must exist", activityMainXml.exists())
        assertTrue(activityMainXml.readText().contains("Hello Calculator!"))

        // Verify AndroidManifest.xml
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifest.exists())
        val manifestContent = manifest.readText()
        assertTrue(manifestContent.contains("android:name=\".MainActivity\""))
        assertTrue(manifestContent.contains("android:label=\"Calculator\""))

        // Verify Gradle files
        val rootSettings = File(projectDir, "settings.gradle.kts")
        assertTrue("settings.gradle.kts must exist", rootSettings.exists())
        assertTrue(rootSettings.readText().contains("rootProject.name = \"Calculator\""))

        val rootBuild = File(projectDir, "build.gradle.kts")
        assertTrue("build.gradle.kts must exist", rootBuild.exists())

        val appBuild = File(projectDir, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts must exist", appBuild.exists())
        val appBuildContent = appBuild.readText()
        assertTrue(appBuildContent.contains("namespace = \"com.example.calculator\""))
        assertTrue(appBuildContent.contains("androidx.appcompat:appcompat"))
        assertTrue(appBuildContent.contains("androidx.constraintlayout:constraintlayout"))

        // Verify resources
        assertTrue(File(projectDir, "app/src/main/res/values/strings.xml").exists())
        assertTrue(File(projectDir, "app/src/main/res/values/colors.xml").exists())
        assertTrue(File(projectDir, "app/src/main/res/values/themes.xml").exists())
    }

    /**
     * TEST 2:
     * Project: Notes
     * Package: com.example.notes
     * Language: Java
     * UI: XML
     *
     * Expected:
     * MainActivity.java
     * activity_main.xml
     * AndroidManifest.xml
     * Gradle files
     * resources
     */
    @Test
    fun testCase2_JavaXmlEmpty_Notes() {
        val config = ProjectConfig(
            projectName = "Notes",
            packageName = "com.example.notes",
            language = "java",
            uiType = "xml",
            activityName = "MainActivity"
        )

        val result = projectManager.generateProject(config, parentDir)
        assertTrue("Generation should succeed: $result", result is GenerationResult.Success)

        val success = result as GenerationResult.Success
        val projectDir = success.projectDir

        // Verify Java source file in app/src/main/java/{{PACKAGE_PATH}}/MainActivity.java
        val mainActivity = File(projectDir, "app/src/main/java/com/example/notes/MainActivity.java")
        assertTrue("MainActivity.java must exist", mainActivity.exists())
        val mainActivityContent = mainActivity.readText()
        assertTrue(mainActivityContent.contains("package com.example.notes;"))
        assertTrue(mainActivityContent.contains("public class MainActivity extends AppCompatActivity"))
        assertTrue(mainActivityContent.contains("setContentView(R.layout.activity_main);"))

        // Verify activity_main.xml
        val activityMainXml = File(projectDir, "app/src/main/res/layout/activity_main.xml")
        assertTrue("activity_main.xml must exist", activityMainXml.exists())

        // Verify AndroidManifest.xml
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifest.exists())
        assertTrue(manifest.readText().contains(".MainActivity"))

        // Verify Gradle files
        val rootSettings = File(projectDir, "settings.gradle")
        assertTrue("settings.gradle must exist", rootSettings.exists())

        val appBuild = File(projectDir, "app/build.gradle")
        assertTrue("app/build.gradle must exist", appBuild.exists())
        val appBuildContent = appBuild.readText()
        assertTrue(appBuildContent.contains("namespace 'com.example.notes'"))
        assertTrue(appBuildContent.contains("androidx.appcompat:appcompat"))

        // Verify resources
        assertTrue(File(projectDir, "app/src/main/res/values/strings.xml").exists())
        assertTrue(File(projectDir, "app/src/main/res/values/colors.xml").exists())
        assertTrue(File(projectDir, "app/src/main/res/values/themes.xml").exists())
    }

    /**
     * TEST 3:
     * Project: Todo
     * Package: com.example.todo
     * Language: Kotlin
     * UI: Compose
     *
     * Expected:
     * MainActivity.kt
     * AndroidManifest.xml
     * Compose dependencies
     *
     * No activity_main.xml.
     */
    @Test
    fun testCase3_KotlinComposeEmpty_Todo() {
        val config = ProjectConfig(
            projectName = "Todo",
            packageName = "com.example.todo",
            language = "kotlin",
            uiType = "compose",
            activityName = "MainActivity"
        )

        val result = projectManager.generateProject(config, parentDir)
        assertTrue("Generation should succeed: $result", result is GenerationResult.Success)

        val success = result as GenerationResult.Success
        val projectDir = success.projectDir

        // Verify MainActivity.kt has Compose activity
        val mainActivity = File(projectDir, "app/src/main/kotlin/com/example/todo/MainActivity.kt")
        assertTrue("MainActivity.kt must exist", mainActivity.exists())
        val mainActivityContent = mainActivity.readText()
        assertTrue(mainActivityContent.contains("package com.example.todo"))
        assertTrue(mainActivityContent.contains("class MainActivity : ComponentActivity()"))
        assertTrue(mainActivityContent.contains("setContent {"))

        // Verify NO activity_main.xml
        val activityMainXml = File(projectDir, "app/src/main/res/layout/activity_main.xml")
        assertFalse("activity_main.xml must NOT exist for Compose projects", activityMainXml.exists())

        // Verify AndroidManifest.xml
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifest.exists())

        // Verify Compose dependencies in Gradle
        val appBuild = File(projectDir, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts must exist", appBuild.exists())
        val appBuildContent = appBuild.readText()
        assertTrue(appBuildContent.contains("compose = true"))
        assertTrue(appBuildContent.contains("androidx.compose.material3:material3"))
        assertTrue(appBuildContent.contains("androidx.activity:activity-compose"))
    }

    /**
     * Test duplicate project detection and safety.
     */
    @Test
    fun testDuplicateProjectSafety() {
        val config = ProjectConfig(
            projectName = "ExistingApp",
            packageName = "com.example.existing",
            language = "kotlin",
            uiType = "xml"
        )

        // First creation succeeds
        val result1 = projectManager.generateProject(config, parentDir)
        assertTrue(result1 is GenerationResult.Success)

        // Second creation without allowOverwrite fails with DuplicateProject
        val result2 = projectManager.generateProject(config, parentDir, allowOverwrite = false)
        assertTrue("Should return DuplicateProject when folder already exists", result2 is GenerationResult.DuplicateProject)

        // With allowOverwrite, it succeeds
        val result3 = projectManager.generateProject(config, parentDir, allowOverwrite = true)
        assertTrue("Should succeed when overwrite allowed", result3 is GenerationResult.Success)
    }

    /**
     * Test validation rules.
     */
    @Test
    fun testValidationRules() {
        // Invalid package name (single segment)
        val badPkg = ProjectConfig(
            projectName = "App",
            packageName = "invalid",
            language = "kotlin",
            uiType = "xml"
        )
        val res1 = ProjectValidator.validateConfig(badPkg)
        assertFalse(res1.isValid)

        // Invalid package name (reserved keyword)
        val badPkg2 = ProjectConfig(
            projectName = "App",
            packageName = "com.class.app",
            language = "kotlin",
            uiType = "xml"
        )
        val res2 = ProjectValidator.validateConfig(badPkg2)
        assertFalse(res2.isValid)

        // Empty project name
        val badName = ProjectConfig(
            projectName = "  ",
            packageName = "com.example.app",
            language = "kotlin",
            uiType = "xml"
        )
        val res3 = ProjectValidator.validateConfig(badName)
        assertFalse(res3.isValid)

        // Java + Compose unsupported
        val javaCompose = ProjectConfig(
            projectName = "JavaCompose",
            packageName = "com.example.jc",
            language = "java",
            uiType = "compose"
        )
        val res4 = ProjectValidator.validateConfig(javaCompose)
        assertFalse(res4.isValid)
    }
}
