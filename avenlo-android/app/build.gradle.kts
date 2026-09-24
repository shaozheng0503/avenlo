plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hotfix.avenlo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hotfix.avenlo"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        // Mock Server 地址：模拟器 10.0.2.2 / 真机改局域网 IP（gradle.properties 可覆盖）
        buildConfigField("String", "API_BASE_URL", "\"${project.findProperty("avenlo.api.base") ?: "http://10.0.2.2:8000"}\"")
        ndk {
            // sherpa-onnx so 库：arm64 真机 + x86_64 模拟器（验证链路用）
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 端侧 STT：sherpa-onnx（离线语音识别，Apache-2.0，JitPack 分发；v 前缀版本才带 Android AAR）
    implementation("com.github.k2-fsa:sherpa-onnx:v1.12.29")
}
