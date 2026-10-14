# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Retrofit interfaces
-keep interface com.ironfleet.urlshortener.data.remote.** { *; }

# Keep Room entities
-keep class com.ironfleet.urlshortener.data.local.** { *; }

# Keep data models used by Gson/Retrofit
-keep class com.ironfleet.urlshortener.data.remote.dto.** { *; }

# Gson / Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
