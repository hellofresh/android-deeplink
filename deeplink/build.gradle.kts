import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.publish.maven.MavenPom

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("com.jfrog.bintray") version Versions.bintrayGradlePlugin
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

dependencies {
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.okio)
    testImplementation(DependenciesTest.junit)
    testImplementation(DependenciesTest.kotlinTest)
    androidTestImplementation(DependenciesTest.junit)
    androidTestImplementation(DependenciesTest.kotlinTest)
    androidTestImplementation(DependenciesTest.supportTestRunner)
}

fun MavenPom.addDependencies() = withXml {
    asNode().appendNode("dependencies").let { depNode ->
        configurations.compile.allDependencies.forEach {
            depNode.appendNode("dependency").apply {
                appendNode("groupId", it.group)
                appendNode("artifactId", it.name)
                appendNode("version", it.version)
            }
        }
    }
}

val artifact = "$buildDir/outputs/aar/${Project.name}-${Project.version}-release.aar"
publishing {

    publications {
        register(Project.artifactId, MavenPublication::class) {
            groupId = Project.groupId
            artifactId = Project.artifactId
            artifact(artifact)
            version = Project.version
            pom.addDependencies()
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER") ?: ""
    key = System.getenv("BINTRAY_API_KEY") ?: ""
    publish = true
    setPublications(Project.artifactId)
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = Project.name
        userOrg = "hf-android-clan"
        websiteUrl = "https://github.com/hellofresh/android-deeplink/"
        githubRepo = "hellofresh/android-deeplink"
        vcsUrl = "https://github.com/hellofresh/android-deeplink/"
        description = "Deeplink parser library"
        setLabels("kotlin", "Android", "Deep link")
        setLicenses("Apache-2.0")
        desc = description
        publish = true
        githubRepo = "hellofresh/android-deeplink"
        githubReleaseNotesFile = "../CHANGELOG.md"
    })
}