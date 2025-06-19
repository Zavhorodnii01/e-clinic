// Top-level build file (for project settings)
// Located in: android/build.gradle.kts

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.1") // Latest stable version
        classpath("com.android.tools.build:gradle:8.2.2") // Android Gradle Plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20") // Kotlin version


    }
}

plugins {
    // These are project-level plugins (apply false)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // DO NOT include com.google.gms.google-services here
}
