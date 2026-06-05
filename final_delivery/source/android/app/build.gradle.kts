import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 读取签名配置（不入库的 local.properties）
val keystoreProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun keystoreProp(key: String, env: String): String =
    keystoreProps.getProperty(key, "").ifEmpty { System.getenv(env) ?: "" }

android {
    namespace = "com.shopping.agent"
    compileSdk = 35
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.shopping.agent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // debug/模拟器 默认连接 localhost。release 构建时通过 -PapiUrl=xxx 覆盖
        val apiUrl = project.findProperty("apiUrl") as String? ?: "http://10.0.2.2:8080"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("hermes-release.jks")
            storePassword = keystoreProp("KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
            keyAlias = keystoreProp("KEY_ALIAS", "KEY_ALIAS")
            keyPassword = keystoreProp("KEY_PASSWORD", "KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android Material (View-based) — 提供 XML 主题如 Theme.Material3.DayNight.NoActionBar
    implementation("com.google.android.material:material:1.12.0")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.3")

    // SplashScreen — 接管系统冷启动白屏动画
    implementation("androidx.core:core-splashscreen:1.0.1")
}
