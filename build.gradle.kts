// 根级 build.gradle.kts — 委托给 apps/android/
// Android Studio 打开 monorepo 根目录时使用
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
