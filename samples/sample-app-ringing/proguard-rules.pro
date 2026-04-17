# CometChat Calls SDK ProGuard Rules
-keep class com.cometchat.calls.** { *; }
-keepclassmembers class com.cometchat.calls.** { *; }

# CometChat Chat SDK ProGuard Rules
-keep class com.cometchat.chat.** { *; }
-keepclassmembers class com.cometchat.chat.** { *; }

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
