@file:Suppress("PropertyName")

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
    id("signing")
    alias(libs.plugins.dokkaAndroid)
}

android {
    compileSdk = 30
    defaultConfig {
        minSdk = 17
        targetSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

tasks.dokkaHtml.configure {
    dokkaSourceSets.named("main") {
        noAndroidSdkLink.set(false)
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaJavadoc"))
}

val GROUP_ID: String by rootProject
val VERSION_NAME: String by rootProject
val REPOSITORY: String by rootProject
val ARTIFACT_ID: String by project
val ARTIFACT_NAME: String by project
val ARTIFACT_DESCRIPTION: String by project

publishing {
    repositories {
        maven {
            name = "Snapshot"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            credentials {
                username = extra.getValue("ossrh.username")
                password = extra.getValue("ossrh.password")
            }
        }
        maven {
            name = "Staging"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = extra.getValue("ossrh.username")
                password = extra.getValue("ossrh.password")
            }
        }
    }
    publications {
        register<MavenPublication>("release") {
            group = GROUP_ID
            version = VERSION_NAME
            groupId = GROUP_ID
            artifactId = ARTIFACT_ID

            afterEvaluate {
                from(components.getByName("release"))
                artifact(sourcesJar)
                artifact(javadocJar)
            }

            pom {
                name.set(ARTIFACT_NAME)
                description.set(ARTIFACT_DESCRIPTION)
                url.set("https://github.com/$REPOSITORY")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("kingsleyadio")
                        name.set("Kingsley Adio")
                        email.set("adiksonline@gmail.com")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("release"))
    isRequired = VERSION_NAME.endsWith("SNAPSHOT").not()
}

inline fun <reified T> ExtraPropertiesExtension.getValue(key: String) = get(key) as T
