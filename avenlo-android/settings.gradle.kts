pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // sherpa-onnx 端侧 STT（JitPack 分发）
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}
rootProject.name = "Avenlo"
include(":app", ":core-domain", ":core-data")

// Kotlin jvmToolchain(17) 自动下载（本机仅 JDK 8/22，无 17）
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
