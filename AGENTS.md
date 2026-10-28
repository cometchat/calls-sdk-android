# CometChat Calls SDK v5 — Android

This repository contains the CometChat Calls SDK sample apps for Android. When working with this codebase, use the agent skills in the `skills/` directory for SDK-specific guidance.

## Skills

Load the relevant skill based on the task:

### Core
- `skills/setup/SKILL.md` — SDK dependencies, Cloudsmith maven, CallAppSettings, init, permissions, Jetifier
- `skills/join-session/SKILL.md` — CometChatCalls.joinSession, SessionSettingsBuilder, voice vs video
- `skills/ringing-integration/SKILL.md` — Calls SDK 5.1 initiateCall / endCall + Push Notifications SDK 2.0: 1:1 and group ringing, incoming calls in every app state
- `skills/session-settings/SKILL.md` — All SessionSettingsBuilder options: layouts, session type, audio mode, hide buttons
- `skills/event-listeners/SKILL.md` — SessionStatus, Participant, Media, ButtonClick, Layout listeners
- `skills/call-logs/SKILL.md` — CallLogRequest, fetching and displaying call history

### Advanced
- `skills/recording/SKILL.md` — Auto-start recording, recording events
- `skills/screen-sharing/SKILL.md` — Screen share viewing, presenter status
- `skills/picture-in-picture/SKILL.md` — PiP mode configuration
- `skills/background-handling/SKILL.md` — CometChatOngoingCallService, foreground service
- `skills/voip-calling/SKILL.md` — Incoming calls built on ConnectionService / PhoneAccount
- `skills/audio-controls/SKILL.md` — Mute/unmute, audio device switching
- `skills/video-controls/SKILL.md` — Camera on/off, switch camera
- `skills/participant-management/SKILL.md` — Participant list, mute/kick, raise hand
- `skills/custom-ui/SKILL.md` — Custom control panel, participant list, layout customization
- `skills/in-call-chat/SKILL.md` — In-call messaging during active session

## Key Rules

- Ringing on Calls SDK 5.1+: `CometChatCalls.initiateCall` / `endCall` and the Push Notifications SDK, not `CometChat.initiateCall`
- Chat SDK signalling (`voip-calling` skill): use `com.cometchat.chat.core.Call` (NOT `com.cometchat.chat.models.Call`)
- `SessionType.VOICE` / `SessionType.VIDEO` (not "AUDIO")
- `LayoutType.TILE` / `LayoutType.SIDEBAR` / `LayoutType.SPOTLIGHT`
- `android.enableJetifier=true` required in gradle.properties
- Cloudsmith maven: `https://dl.cloudsmith.io/public/cometchat/cometchat/maven/`
- Language: Kotlin, Java 17. Build: Gradle KTS, minSdk 26, compileSdk 35 (Calls SDK only, `sample-app`); with the Push Notifications SDK 2.0, compileSdk 36 and AGP 8.9.1+ (`sample-app-ringing`)
- Documentation: https://www.cometchat.com/docs/calls/android/overview
