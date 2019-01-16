import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()

    }
    dependencies {
        classpath(Dependencies.androidGradlePlugin)
        classpath(kotlin("gradle-plugin", version = Versions.kotlin))
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version Versions.detekt
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

detekt {
    input = files("src/main/kotlin")
    filters = ".*/resources/.*,.*/build/.*"
}
