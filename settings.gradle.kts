// Monorepo root — 指向 apps/android 子项目
// 这样 Android Studio 打开 04-rag-ecommerce 根目录也能识别 Gradle 项目
rootProject.name = "rag-ecommerce-agent"

include(":app")
project(":app").projectDir = file("apps/android/app")
