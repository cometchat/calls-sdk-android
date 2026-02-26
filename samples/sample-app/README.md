
<p align="center">
  <img alt="CometChat" src="https://assets.cometchat.io/website/images/logos/banner.png">
</p>

# Android Calls Sample App by CometChat

This is a reference application showcasing the integration of [CometChat's Android Calls SDK](https://www.cometchat.com/docs/calls/android/overview) in a native Android project. It demonstrates how to implement real-time voice and video calling features with ease.

<p align="center">
  <img src="../../screenshots/showcase-1.png" alt="Mobile Screenshot 1" width="30%">&nbsp;&nbsp;
  <img src="../../screenshots/showcase-2.png" alt="Mobile Screenshot 2" width="30%">&nbsp;&nbsp;
  <img src="../../screenshots/showcase-3.png" alt="Mobile Screenshot 3" width="30%">
</p>


## Prerequisites

Sign up for a [CometChat](https://app.cometchat.com/) account to obtain your app credentials: _`App ID`_, _`Region`_, and _`Auth Key`_

- Android Studio (latest stable version)
- JDK 17 or later
- Android device or emulator with API level 26+


## Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/cometchat/calls-sdk-android.git
   ```

1. Open the project in Android Studio.

1. Sync Gradle to download all dependencies.

1. `[Optional]` Configure CometChat credentials:
    - Open the `AppConstants.kt` file located at `samples/sample-app/src/main/kotlin/com/cometchat/samplecalls/utils/AppConstants.kt` and enter your CometChat _`APP_ID`_, _`REGION`_, and _`AUTH_KEY`_:
      ```kotlin
      object AppConstants {
          const val APP_ID = "YOUR_APP_ID"
          const val REGION = "YOUR_REGION"  // us, eu, or in
          const val AUTH_KEY = "YOUR_AUTH_KEY"
      }
      ```
    - Alternatively, you can enter your credentials on first launch via the in-app credentials screen.

1. Run the app on a device or emulator.


## Help and Support

For issues running the project or integrating with our Calls SDK, consult our [documentation](https://www.cometchat.com/docs/calls/android/overview) or create a [support ticket](https://help.cometchat.com/hc/en-us). You can also access real-time support via the [CometChat Dashboard](http://app.cometchat.com/).
