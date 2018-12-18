object Project {

    const val artifactId = "deeplink"
    const val version = "1.0.0-SNAPSHOT"
    const val groupId = "com.hellofresh.android"
    const val name = "android-$artifactId"
}

object Android {
    const val sdk = 28
    const val minSdk = 17
}

object Versions {

    const val androidGradlePlugin = "3.4.0-alpha04"
    const val detekt = "1.0.0-RC12"
    const val junit = "4.12"
    const val kotlin = "1.3.11"
}

object Dependencies {

    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    const val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
}

object DependenciesTest {

    const val junit = "junit:junit:${Versions.junit}"
}