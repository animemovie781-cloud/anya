package com.amaya.intelligence.appbuilder.engine

/**
 * Generates root and module-level Gradle configuration files for Android projects.
 */
object GradleConfigGenerator {

    /**
     * Generates the root settings.gradle or settings.gradle.kts file content.
     */
    fun generateRootSettings(config: ProjectConfig): TemplateFile {
        val isKotlinDsl = config.normalizedLanguage() == "kotlin"
        val fileName = if (isKotlinDsl) "settings.gradle.kts" else "settings.gradle"

        val content = if (isKotlinDsl) {
            """
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "${config.projectName}"
include(":app")
""".trimIndent()
        } else {
            """
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "${config.projectName}"
include ':app'
""".trimIndent()
        }

        return TemplateFile(fileName, content)
    }

    /**
     * Generates root build.gradle.kts or build.gradle.
     */
    fun generateRootBuild(config: ProjectConfig): TemplateFile {
        val isKotlinDsl = config.normalizedLanguage() == "kotlin"
        val isCompose = config.normalizedUiType() == "compose"
        val fileName = if (isKotlinDsl) "build.gradle.kts" else "build.gradle"

        val content = if (isKotlinDsl) {
            if (isCompose) {
                """
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
""".trimIndent()
            } else {
                """
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
""".trimIndent()
            }
        } else {
            """
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '8.9.1' apply false
}
""".trimIndent()
        }

        return TemplateFile(fileName, content)
    }

    /**
     * Generates gradle.properties.
     */
    fun generateGradleProperties(): TemplateFile {
        val content = """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
""".trimIndent()
        return TemplateFile("gradle.properties", content)
    }

    /**
     * Generates gradle/wrapper/gradle-wrapper.properties.
     */
    fun generateGradleWrapperProperties(): TemplateFile {
        val content = """
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""".trimIndent()
        return TemplateFile("gradle/wrapper/gradle-wrapper.properties", content)
    }

    /**
     * Generates app/build.gradle.kts or app/build.gradle.
     */
    fun generateAppBuild(config: ProjectConfig): TemplateFile {
        val isKotlin = config.normalizedLanguage() == "kotlin"
        val isCompose = isKotlin && config.normalizedUiType() == "compose"
        val fileName = if (isKotlin) "app/build.gradle.kts" else "app/build.gradle"

        val content = when {
            isCompose -> generateComposeAppBuild(config)
            isKotlin -> generateKotlinXmlAppBuild(config)
            else -> generateJavaXmlAppBuild(config)
        }

        return TemplateFile(fileName, content)
    }

    private fun generateComposeAppBuild(config: ProjectConfig): String {
        return """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "${config.packageName}"
    compileSdk = ${config.compileSdk}

    defaultConfig {
        applicationId = "${config.packageName}"
        minSdk = ${config.minSdk}
        targetSdk = ${config.targetSdk}
        versionCode = ${config.versionCode}
        versionName = "${config.versionName}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
""".trimIndent()
    }

    private fun generateKotlinXmlAppBuild(config: ProjectConfig): String {
        return """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "${config.packageName}"
    compileSdk = ${config.compileSdk}

    defaultConfig {
        applicationId = "${config.packageName}"
        minSdk = ${config.minSdk}
        targetSdk = ${config.targetSdk}
        versionCode = ${config.versionCode}
        versionName = "${config.versionName}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
""".trimIndent()
    }

    private fun generateJavaXmlAppBuild(config: ProjectConfig): String {
        return """
plugins {
    id 'com.android.application'
}

android {
    namespace '${config.packageName}'
    compileSdk ${config.compileSdk}

    defaultConfig {
        applicationId '${config.packageName}'
        minSdk ${config.minSdk}
        targetSdk ${config.targetSdk}
        versionCode ${config.versionCode}
        versionName '${config.versionName}'

        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.0'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
}
""".trimIndent()
    }
}
