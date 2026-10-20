pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat/maven/") }
        // Push notifications SDKs (push-notifications-android / push-notifications-calls-android).
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/call-team/maven/") }
    }
}

rootProject.name = "calls-sdk-android"

include(":samples:sample-app")
include(":samples:sample-app-ringing")
include(":samples:sample-app-voip")
