# SysMetrics ProGuard Rules - Release Optimization

# Aggressive optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep application class
-keep class com.sysmetrics.app.SysMetricsApp { *; }

# Keep data models (for serialization)
-keep class com.sysmetrics.app.data.model.** { *; }
-keep class com.sysmetrics.app.domain.model.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep services
-keep class * extends android.app.Service
-keep class com.sysmetrics.app.service.** { *; }

# Keep broadcast receivers
-keep class * extends android.content.BroadcastReceiver

# Hilt DI
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

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
