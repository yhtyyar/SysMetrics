# SysMetrics ProGuard Rules

# Keep application class
-keep class com.sysmetrics.app.SysMetricsApp { *; }

# Keep data models
-keep class com.sysmetrics.app.data.model.** { *; }
-keep class com.sysmetrics.app.domain.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Timber
-dontwarn org.jetbrains.annotations.**
