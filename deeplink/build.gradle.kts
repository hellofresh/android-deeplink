val kotlin_version: String by extra
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}
apply {
    plugin("kotlin-android")
    plugin("kotlin-android-extensions")
}


android {
    compileSdkVersion(Android.sdk)
    defaultConfig {
        minSdkVersion(Android.minSdk)
        targetSdkVersion(Android.sdk)
        versionName = Project.version
        setProperty("archivesBaseName", "${Project.name}")
    }
}

project.group = Project.groupId
project.version = Project.version

publishing {

    publications {
        register("${Project.artifactId}", MavenPublication::class) {
            groupId = Project.groupId
            artifactId = Project.artifactId
            artifact("$buildDir/outputs/aar/${Project.name}-${Project.version}.aar")
            version = Project.version
        }
    }
}

dependencies {
    implementation(Dependencies.kotlinStdLib)
    testImplementation(DependenciesTest.junit)
}
repositories {
    mavenCentral()
}