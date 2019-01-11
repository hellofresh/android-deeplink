plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("com.github.breadmoirai.github-release") version Versions.githubReleaseGradlePlugin
}

android {
    compileSdkVersion(Android.sdk)
    defaultConfig {
        minSdkVersion(Android.minSdk)
        targetSdkVersion(Android.sdk)
        versionName = Project.version
        setProperty("archivesBaseName", "${Project.name}-${Project.version}")
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

group = Project.groupId
version = Project.version

val artifact = "$buildDir/outputs/aar/${Project.name}-${Project.version}-release.aar"

publishing {

    publications {
        register(Project.artifactId, MavenPublication::class) {
            groupId = Project.groupId
            artifactId = Project.artifactId
            artifact(artifact)
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

githubRelease {
    token(System.getenv("GITHUB_TOKEN") ?: "")
    owner("hellofresh")
    repo("android-deeplink")
    releaseAssets(artifact)
    setOverwrite(true)
    setBody("")
}