# CometChat Calls SDK v5 — Android

This repository contains the CometChat Calls SDK sample apps for Android. When working with this codebase, use the agent skills in the `skills/` directory for SDK-specific guidance.

## Skills

Load the relevant skill based on the task:

### Core
- `skills/setup/SKILL.md` — SDK dependencies, Cloudsmith maven, CallAppSettings, init, permissions, Jetifier
- `skills/join-session/SKILL.md` — CometChatCalls.joinSession, SessionSettingsBuilder, voice vs video
- `skills/ringing-integration/SKILL.md` — Dual SDK (Chat + Calls), initiateCall, accept/reject/cancel
- `skills/session-settings/SKILL.md` — All SessionSettingsBuilder options: layouts, session type, audio mode, hide buttons
- `skills/event-listeners/SKILL.md` — SessionStatus, Participant, Media, ButtonClick, Layout listeners
- `skills/call-logs/SKILL.md` — CallLogRequest, fetching and displaying call history

### Advanced
- `skills/recording/SKILL.md` — Auto-start recording, recording events
- `skills/screen-sharing/SKILL.md` — Screen share viewing, presenter status
- `skills/picture-in-picture/SKILL.md` — PiP mode configuration
- `skills/background-handling/SKILL.md` — CometChatOngoingCallService, foreground service
- `skills/voip-calling/SKILL.md` — VoIP push notifications, ConnectionService, PhoneAccount
- `skills/audio-controls/SKILL.md` — Mute/unmute, audio device switching
- `skills/video-controls/SKILL.md` — Camera on/off, switch camera
- `skills/participant-management/SKILL.md` — Participant list, mute/kick, raise hand
- `skills/custom-ui/SKILL.md` — Custom control panel, participant list, layout customization
- `skills/in-call-chat/SKILL.md` — In-call messaging during active session

## Key Rules

- Use `com.cometchat.chat.core.Call` (NOT `com.cometchat.chat.models.Call`)
- `SessionType.VOICE` / `SessionType.VIDEO` (not "AUDIO")
- `LayoutType.TILE` / `LayoutType.SIDEBAR` / `LayoutType.SPOTLIGHT`
- `android.enableJetifier=true` required in gradle.properties
- Cloudsmith maven: `https://dl.cloudsmith.io/public/cometchat/cometchat/maven/`
- Language: Kotlin, Java 17. Build: Gradle KTS, compileSdk 35, minSdk 26
- Documentation: https://www.cometchat.com/docs/calls/android/overview
