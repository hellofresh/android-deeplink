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
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish.base")
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

mavenPublishing {
    configure(
        com.vanniktech.maven.publish.AndroidLibrary(
            com.vanniktech.maven.publish.JavadocJar.Dokka("dokkaJavadoc")
        )
    )
}
