# Nebula Executor ProGuard Rules
# Keep OkHttp (WebSocket engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep all app classes (small app, keep everything)
-keep class com.nebula.executor.** { *; }
