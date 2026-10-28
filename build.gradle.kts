// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // AGP 8.9.1+ and Kotlin 2.1 are required by the push notifications SDK's androidx dependencies.
    id("com.android.application") version "8.11.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
