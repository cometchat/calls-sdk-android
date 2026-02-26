# CometChat Calls SDK ProGuard Rules
# Add these rules to your app's proguard-rules.pro file

# Keep CometChat Calls SDK classes
-keep class com.cometchat.calls.** { *; }
-keepclassmembers class com.cometchat.calls.** { *; }

# Keep WebRTC classes
-keep class org.webrtc.** { *; }

# Keep model classes for JSON serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Keep generic signatures for Gson
-keepattributes Signature

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
