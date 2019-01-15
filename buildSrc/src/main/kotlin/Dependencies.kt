object Project {

    const val artifactId = "deeplink"
    const val version = "0.1.0"
    const val groupId = "com.hellofresh.android"
    const val name = "android-$artifactId"
}

object Android {
    const val sdk = 28
    const val minSdk = 17
}

object Versions {

    const val androidGradlePlugin = "3.4.0-alpha10"
    const val detekt = "1.0.0-RC12"
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
    const val supportTestRunner = "com.android.support.test:runner:${Versions.supportTest}"
}
