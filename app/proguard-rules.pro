# SysMetrics ProGuard Rules - Release Optimization

# Aggressive optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep application class
-keep class com.sysmetrics.app.SysMetricsApp { *; }
-keep class com.sysmetrics.app.core.SysMetricsApplication { *; }

# Keep data models (for serialization)
-keep class com.sysmetrics.app.data.model.** { *; }
-keep class com.sysmetrics.app.domain.model.** { *; }

# Keep native bridge classes and methods
-keep class com.sysmetrics.app.native_bridge.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep JNI interfaces
-keep interface com.sysmetrics.app.domain.collector.** { *; }
-keep interface com.sysmetrics.app.domain.formatter.** { *; }

# Keep services
-keep class * extends android.app.Service
-keep class com.sysmetrics.app.service.** { *; }

# Keep broadcast receivers
-keep class * extends android.content.BroadcastReceiver

# Manual DI (AppContainer)
-keep class com.sysmetrics.app.core.di.AppContainer { *; }
-keep class com.sysmetrics.app.core.di.DispatcherProvider { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# Lifecycle
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Timber
-dontwarn org.jetbrains.annotations.**
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
