plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()

    // ── iOS cinterop for MpvPlayerBridge ──
    // Resolves the Objective-C header so Kotlin/Native can call into the Swift mpv bridge.
    // Requires Xcode with iOS SDK to be installed (xcrun --sdk iphonesimulator --show-sdk-path).
    val mpvBridgeDef = project.file("src/nativeInterop/cinterop/bridge.def")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "player"
            isStatic = true
        }
        iosTarget.compilations.getByName("main") {
            val cinterop by cinterops.creating {
                defFile(mpvBridgeDef)
                packageName("mpvbridge")
                // Point clang at the iOS SDK so UIKit/UIKit.h is found
                val sdkPath =
                    when {
                        iosTarget.name.contains("simulator", ignoreCase = true) ->
                            "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk"
                        else ->
                            "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS.sdk"
                    }
                val headerDir = project.file("src/nativeInterop/cinterop").absolutePath
                compilerOpts("-isysroot", sdkPath, "-I$headerDir")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:model"))
            implementation(project(":core:player-api"))
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.material.icons.extended)
        }
        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.exoplayer.smoothstreaming)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.cast)
            implementation(libs.androidx.mediarouter)
            implementation(libs.play.services.cast.framework)
            implementation(libs.kotlin.reflect)
            implementation(libs.okhttp)
            implementation(libs.androidx.media3.datasource.okhttp)
        }
    }
}

android {
    namespace = "com.moviehub.feature.player"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
