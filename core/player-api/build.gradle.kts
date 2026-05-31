plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "core-player-api"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.moviehub.core.player"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
