import groovy.lang.GroovyObject
import org.jetbrains.dokka.gradle.DokkaTask
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig

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
    kotlin("android")
    id("de.mobilej.unmock")
    id("maven-publish")
    alias(libs.plugins.bintray)
    alias(libs.plugins.dokkaAndroid)
    alias(libs.plugins.jfrogArtifactory)
}

android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(17)
        targetSdkVersion(30)
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.kotlinStdlib)
    implementation(libs.okio)

    unmock(libs.robolectricAndroid)
    testImplementation(libs.kotlinTestJunit)
}

unMock {
    keep("android.net.Uri")
    keepStartingWith("libcore.")
    keepAndRename("java.nio.charset.Charsets").to("xjava.nio.charset.Charsets")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(dokka)
}

val GROUP_ID: String by rootProject
val VERSION_NAME: String by rootProject
val ARTIFACT_ID: String by project

publishing {
    publications {
        register<MavenPublication>("release") {
            group = GROUP_ID
            version = VERSION_NAME
            groupId = GROUP_ID
            artifactId = ARTIFACT_ID

            afterEvaluate {
                from(components.getByName("release"))
                artifact(sourcesJar)
                artifact(dokkaJar)
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER") ?: ""
    key = System.getenv("BINTRAY_API_KEY") ?: ""
    publish = true
    setPublications(ARTIFACT_ID)
    with(pkg) {
        repo = "maven"
        name = rootProject.name
        websiteUrl = "https://github.com/kingsleyadio/android-deeplink/"
        githubRepo = "kingsleyadio/android-deeplink"
        vcsUrl = "https://github.com/kingsleyadio/android-deeplink/"
        description = "Deeplink parser library"
        desc = description
        publish = true
        githubRepo = "kingsleyadio/android-deeplink"
        githubReleaseNotesFile = "../CHANGELOG.md"
        version.name = VERSION_NAME
        setLabels("kotlin", "Android", "Deep link")
        setLicenses("Apache-2.0")
    }
}

artifactory {
    setContextUrl("https://oss.jfrog.org")
    publish(delegateClosureOf<PublisherConfig> {
        repository(delegateClosureOf<GroovyObject> {
            val repoKey = if (VERSION_NAME.endsWith("SNAPSHOT")) "oss-snapshot-local" else "oss-release-local"
            setProperty("repoKey", repoKey)
            setProperty("username", System.getenv("BINTRAY_USER") ?: "")
            setProperty("password", System.getenv("BINTRAY_API_KEY") ?: "")
            setProperty("maven", true)
        })
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", ARTIFACT_ID)
        })

        resolve(delegateClosureOf<ResolverConfig> {
            setProperty("repoKey", "jcenter")
        })
    })
}
