plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

android {
    compileSdkVersion(Android.sdk)
    defaultConfig {
        minSdkVersion(Android.minSdk)
        targetSdkVersion(Android.sdk)
        versionName = Project.version
        setProperty("archivesBaseName", Project.name)
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

group = Project.groupId
version = Project.version

publishing {

    publications {
        register(Project.artifactId, MavenPublication::class) {
            groupId = Project.groupId
            artifactId = Project.artifactId
            artifact("$buildDir/outputs/aar/${Project.name}-${Project.version}.aar")
            version = Project.version
        }
    }
}

dependencies {
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.okio)
    testImplementation(DependenciesTest.junit)
    testImplementation(DependenciesTest.kotlinTest)
    androidTestImplementation(DependenciesTest.junit)
    androidTestImplementation(DependenciesTest.kotlinTest)
    androidTestImplementation(DependenciesTest.supportTestRunner)
}

repositories {
    mavenCentral()
}
