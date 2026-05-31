plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "navigation"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:home"))
            implementation(project(":feature:search"))
            implementation(project(":feature:addon"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:details"))
            implementation(project(":feature:player"))
            implementation(project(":feature:sync"))
            implementation(project(":feature:profile"))
            implementation(project(":core:model"))
            implementation(project(":core:database"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.material.icons.extended)
        }
    }
}

android {
    namespace = "com.moviehub.navigation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
