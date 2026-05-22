rootProject.name = "MovieHub"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":core:ui")
include(":core:utils")
include(":core:model")
include(":core:network")
include(":core:database")
include(":navigation")
include(":di")
include(":feature:home")
include(":feature:search")
include(":feature:addon")
include(":feature:details")
include(":feature:auth")
include(":feature:player")
include(":feature:sync")
include(":feature:profile")
