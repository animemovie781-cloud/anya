package com.amaya.intelligence.appbuilder.engine

/**
 * Generates the AndroidManifest.xml file for generated projects.
 */
object ManifestGenerator {

    /**
     * Generates AndroidManifest.xml for the project.
     */
    fun generate(config: ProjectConfig): TemplateFile {
        val appName = if (config.appName.isNotBlank()) config.appName else config.projectName
        val activityClass = if (config.activityName.startsWith(".")) config.activityName else ".${config.activityName}"

        val content = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$appName"
        android:roundIcon="@mipmap/ic_launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.${config.projectName.replace(" ", "")}">
        <activity
            android:name="$activityClass"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
""".trimIndent()

        return TemplateFile("app/src/main/AndroidManifest.xml", content)
    }
}
