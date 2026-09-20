// Avenlo 根构建文件 —— Anker 黑客松 Hotfix 队
// 全部插件在 root 统一声明 apply false，子模块经 alias 引用（避免 classpath 版本冲突）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
