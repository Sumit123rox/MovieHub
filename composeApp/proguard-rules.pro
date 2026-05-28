# Keep line numbers and source file names for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin Metadata
-keepattributes *Annotation*,Signature,InnerClasses

# Serialization
-keep class kotlinx.serialization.** { *; }

# Media3/ExoPlayer
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Networking (Ktor / OkHttp)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**
-keep class okhttp3.** { *; }

# Keep QuickJS if we add plugin support
-keep class com.nuvio.app.features.plugins.** { *; }

# General R8 hardening
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Strip verbose/debug Kermit logs in release
-assumenosideeffects class co.touchlab.kermit.Logger {
    public void v(...);
    public void d(...);
}
