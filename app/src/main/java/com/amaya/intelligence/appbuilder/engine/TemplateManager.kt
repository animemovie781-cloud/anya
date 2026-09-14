package com.amaya.intelligence.appbuilder.engine

import android.content.Context
import java.io.File
import java.io.InputStream

/**
 * Manages project templates, loading metadata and starter files.
 */
class TemplateManager(private val context: Context? = null) {

    private val templates = mutableMapOf<String, TemplateMetadata>()
    private val templateFilesProvider = mutableMapOf<String, (ProjectConfig) -> List<TemplateFile>>()

    init {
        registerBuiltInTemplates()
        if (context != null) {
            loadFromAssets(context)
        }
    }

    /**
     * Registers a template metadata and its starter files generator.
     */
    fun registerTemplate(
        metadata: TemplateMetadata,
        filesProvider: (ProjectConfig) -> List<TemplateFile>
    ) {
        templates[metadata.templateId] = metadata
        templateFilesProvider[metadata.templateId] = filesProvider
    }

    fun getTemplate(templateId: String): TemplateMetadata? = templates[templateId]

    fun listTemplates(): List<TemplateMetadata> = templates.values.toList()

    /**
     * Resolves template by language and UI type.
     */
    fun resolveTemplate(language: String, uiType: String): TemplateMetadata {
        val normLang = language.trim().lowercase()
        val normUi = uiType.trim().lowercase()

        val id = when {
            normLang == "kotlin" && normUi == "compose" -> "kotlin_compose_empty"
            normLang == "kotlin" && normUi == "xml" -> "kotlin_xml_empty"
            normLang == "java" && normUi == "xml" -> "java_xml_empty"
            normLang == "java" -> "java_xml_empty"
            else -> "kotlin_xml_empty"
        }

        return templates[id] ?: templates["kotlin_xml_empty"]
            ?: throw IllegalStateException("Default template kotlin_xml_empty not found")
    }

    /**
     * Returns starter files for a given template and project configuration.
     */
    fun getStarterFiles(config: ProjectConfig): List<TemplateFile> {
        val templateId = config.resolveTemplateId()
        val provider = templateFilesProvider[templateId]
            ?: templateFilesProvider["kotlin_xml_empty"]
            ?: throw IllegalArgumentException("No template found for ID: $templateId")
        return provider(config)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Built-in Templates Setup
    // ─────────────────────────────────────────────────────────────────────────────

    private fun registerBuiltInTemplates() {
        // 1. Kotlin + XML
        val kotlinXmlMeta = TemplateMetadata(
            templateId = "kotlin_xml_empty",
            name = "Empty Views Activity (Kotlin)",
            language = "kotlin",
            uiType = "xml",
            minimumSdk = 24,
            requiredFiles = listOf(
                "app/src/main/kotlin/{{PACKAGE_PATH}}/{{ACTIVITY_NAME}}.kt",
                "app/src/main/res/layout/activity_main.xml",
                "app/src/main/res/values/strings.xml",
                "app/src/main/res/values/colors.xml",
                "app/src/main/res/values/themes.xml"
            ),
            directories = listOf(
                "app/src/main/kotlin/{{PACKAGE_PATH}}",
                "app/src/main/res/layout",
                "app/src/main/res/values",
                "app/src/main/res/drawable",
                "app/src/main/res/mipmap"
            ),
            dependencies = listOf(
                "androidx.core:core-ktx:1.15.0",
                "androidx.appcompat:appcompat:1.7.0",
                "com.google.android.material:material:1.12.0",
                "androidx.constraintlayout:constraintlayout:2.2.0"
            )
        )
        registerTemplate(kotlinXmlMeta) { config ->
            buildKotlinXmlFiles(config)
        }

        // 2. Kotlin + Compose
        val kotlinComposeMeta = TemplateMetadata(
            templateId = "kotlin_compose_empty",
            name = "Empty Compose Activity (Kotlin)",
            language = "kotlin",
            uiType = "compose",
            minimumSdk = 24,
            requiredFiles = listOf(
                "app/src/main/kotlin/{{PACKAGE_PATH}}/{{ACTIVITY_NAME}}.kt",
                "app/src/main/res/values/strings.xml",
                "app/src/main/res/values/colors.xml",
                "app/src/main/res/values/themes.xml"
            ),
            directories = listOf(
                "app/src/main/kotlin/{{PACKAGE_PATH}}",
                "app/src/main/res/values",
                "app/src/main/res/drawable",
                "app/src/main/res/mipmap"
            ),
            dependencies = listOf(
                "androidx.core:core-ktx:1.15.0",
                "androidx.lifecycle:lifecycle-runtime-ktx:2.8.7",
                "androidx.activity:activity-compose:1.10.0",
                "androidx.compose.material3:material3"
            )
        )
        registerTemplate(kotlinComposeMeta) { config ->
            buildKotlinComposeFiles(config)
        }

        // 3. Java + XML
        val javaXmlMeta = TemplateMetadata(
            templateId = "java_xml_empty",
            name = "Empty Views Activity (Java)",
            language = "java",
            uiType = "xml",
            minimumSdk = 24,
            requiredFiles = listOf(
                "app/src/main/java/{{PACKAGE_PATH}}/{{ACTIVITY_NAME}}.java",
                "app/src/main/res/layout/activity_main.xml",
                "app/src/main/res/values/strings.xml",
                "app/src/main/res/values/colors.xml",
                "app/src/main/res/values/themes.xml"
            ),
            directories = listOf(
                "app/src/main/java/{{PACKAGE_PATH}}",
                "app/src/main/res/layout",
                "app/src/main/res/values",
                "app/src/main/res/drawable",
                "app/src/main/res/mipmap"
            ),
            dependencies = listOf(
                "androidx.appcompat:appcompat:1.7.0",
                "com.google.android.material:material:1.12.0",
                "androidx.constraintlayout:constraintlayout:2.2.0"
            )
        )
        registerTemplate(javaXmlMeta) { config ->
            buildJavaXmlFiles(config)
        }
    }

    private fun buildKotlinXmlFiles(config: ProjectConfig): List<TemplateFile> {
        val pkgPath = PackagePathGenerator.toPackagePath(config.packageName)
        val activityPath = "app/src/main/kotlin/$pkgPath/${config.activityName}.kt"

        val activityContent = """
package ${config.packageName}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ${config.activityName} : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
""".trimIndent()

        val layoutContent = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".${config.activityName}">

    <TextView
        android:id="@+id/tv_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello ${config.projectName}!"
        android:textSize="20sp"
        android:textColor="@color/purple_700"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
""".trimIndent()

        return listOf(
            TemplateFile(activityPath, activityContent),
            TemplateFile("app/src/main/res/layout/activity_main.xml", layoutContent),
            buildStringsXml(config),
            buildColorsXml(),
            buildThemesXml(config)
        )
    }

    private fun buildJavaXmlFiles(config: ProjectConfig): List<TemplateFile> {
        val pkgPath = PackagePathGenerator.toPackagePath(config.packageName)
        val activityPath = "app/src/main/java/$pkgPath/${config.activityName}.java"

        val activityContent = """
package ${config.packageName};

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ${config.activityName} extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
""".trimIndent()

        val layoutContent = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".${config.activityName}">

    <TextView
        android:id="@+id/tv_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello ${config.projectName}!"
        android:textSize="20sp"
        android:textColor="@color/purple_700"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
""".trimIndent()

        return listOf(
            TemplateFile(activityPath, activityContent),
            TemplateFile("app/src/main/res/layout/activity_main.xml", layoutContent),
            buildStringsXml(config),
            buildColorsXml(),
            buildThemesXml(config)
        )
    }

    private fun buildKotlinComposeFiles(config: ProjectConfig): List<TemplateFile> {
        val pkgPath = PackagePathGenerator.toPackagePath(config.packageName)
        val activityPath = "app/src/main/kotlin/$pkgPath/${config.activityName}.kt"

        val activityContent = """
package ${config.packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class ${config.activityName} : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "${config.projectName}",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello ${'$'}name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        Greeting("Android")
    }
}
""".trimIndent()

        return listOf(
            TemplateFile(activityPath, activityContent),
            buildStringsXml(config),
            buildColorsXml(),
            buildThemesXml(config)
        )
    }

    private fun buildStringsXml(config: ProjectConfig): TemplateFile {
        val appName = if (config.appName.isNotBlank()) config.appName else config.projectName
        val content = """
<resources>
    <string name="app_name">$appName</string>
</resources>
""".trimIndent()
        return TemplateFile("app/src/main/res/values/strings.xml", content)
    }

    private fun buildColorsXml(): TemplateFile {
        val content = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
""".trimIndent()
        return TemplateFile("app/src/main/res/values/colors.xml", content)
    }

    private fun buildThemesXml(config: ProjectConfig): TemplateFile {
        val themeName = config.projectName.replace(" ", "")
        val content = """
<resources>
    <style name="Theme.$themeName" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
    </style>
</resources>
""".trimIndent()
        return TemplateFile("app/src/main/res/values/themes.xml", content)
    }

    private fun loadFromAssets(ctx: Context) {
        try {
            val assetPaths = listOf(
                "templates/android/kotlin/xml_empty/template.json",
                "templates/android/kotlin/compose_empty/template.json",
                "templates/android/java/xml_empty/template.json"
            )
            for (path in assetPaths) {
                runCatching {
                    ctx.assets.open(path).use { input ->
                        val metadata = TemplateParser.parse(input)
                        templates[metadata.templateId] = metadata
                    }
                }
            }
        } catch (_: Exception) {
            // Assets not found or asset manager error, built-ins remain active
        }
    }
}
