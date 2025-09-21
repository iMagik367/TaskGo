// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// Enforce Java Toolchain 17 for all subprojects
subprojects {
    extensions.findByName("java")?.let {
        (it as org.gradle.api.plugins.JavaPluginExtension).toolchain {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
        }
    }
}