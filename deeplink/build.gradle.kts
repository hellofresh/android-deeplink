
import org.gradle.api.publish.maven.MavenPom
import org.jetbrains.dokka.gradle.DokkaTask

/*
 * Copyright (c) 2019.  The HelloFresh Android Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("com.jfrog.bintray") version Versions.bintrayGradlePlugin
    id("org.jetbrains.dokka-android") version Versions.dokkaAndroid
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
    testImplementation(DependenciesTest.kotlinTestJunit)
    androidTestImplementation(DependenciesTest.junit)
    androidTestImplementation(DependenciesTest.kotlinTest)
    androidTestImplementation(DependenciesTest.kotlinTestJunit)
    androidTestImplementation(DependenciesTest.supportTestRunner)
}

val artifact = "$buildDir/outputs/aar/${Project.name}-${Project.version}-release.aar"

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(dokka)
}

artifacts {
    archives(sourcesJar)
    archives(dokkaJar)
}


publishing {

    publications {
        register(Project.artifactId, MavenPublication::class) {
            groupId = Project.groupId
            artifactId = Project.artifactId
            version = Project.version
            artifact(artifact)
            artifact(sourcesJar)
            artifact(dokkaJar)
            pom.addDependencies()
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER") ?: ""
    key = System.getenv("BINTRAY_API_KEY") ?: ""
    publish = true
    setPublications(Project.artifactId)
    with(pkg) {
        repo = "maven"
        name = Project.name
        websiteUrl = "https://github.com/hellofresh/android-deeplink/"
        githubRepo = "hellofresh/android-deeplink"
        vcsUrl = "https://github.com/hellofresh/android-deeplink/"
        description = "Deeplink parser library"
        desc = description
        publish = true
        githubRepo = "hellofresh/android-deeplink"
        githubReleaseNotesFile = "../CHANGELOG.md"
        with(version) {
            name = Project.version
        }
        setLabels("kotlin", "Android", "Deep link")
        setLicenses("Apache-2.0")
    }
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
