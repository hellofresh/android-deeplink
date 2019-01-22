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

object Project {

    const val artifactId = "deeplink"
    const val version = "0.2.0-SNAPSHOT"
    const val groupId = "com.hellofresh.android"
    const val name = "android-$artifactId"
}

object Android {
    const val sdk = 28
    const val minSdk = 17
}

object Versions {

    const val androidGradlePlugin = "3.4.0-alpha10"
    const val bintrayGradlePlugin = "1.8.4"
    const val detekt = "1.0.0-RC12"
    const val dokkaAndroid = "0.9.17"
    const val junitJacoco = "0.13.0"
    const val junit = "4.12"
    const val kotlin = "1.3.11"
    const val okio = "2.1.0"
    const val supportTest = "1.0.2"
}

object Dependencies {

    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    const val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val okio = "com.squareup.okio:okio:${Versions.okio}"
}

object DependenciesTest {

    const val junit = "junit:junit:${Versions.junit}"
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    const val supportTestRunner = "com.android.support.test:runner:${Versions.supportTest}"
}
