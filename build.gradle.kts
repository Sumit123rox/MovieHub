plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
}

configure(subprojects.filter { it.path in listOf(":core:player-api", ":feature:player") }) {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint("1.2.1")
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("1.2.1")
        }
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        toolVersion = "1.23.6"
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/detekt.yml"))
    }
}
