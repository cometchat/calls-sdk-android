---
name: migration-v4-to-v5
description: Migrate an Android project from CometChat Calls SDK v4 to v5. Use when upgrading calls SDK version, replacing deprecated v4 APIs, migrating CallSettings to SessionSettings, replacing CometChatCallsEventsListener with v5 listeners, migrating startSession to joinSession, or adding CometChatCalls.login(). Triggers on "migrate calls sdk", "upgrade v4 to v5", "replace deprecated calls api", "CallSettings to SessionSettings", "migration guide".
inclusion: manual
---

# CometChat Calls SDK — Migrate v4 to v5 (Android)

## Overview

Calls SDK v5 is a drop-in replacement for v4. All v4 APIs are preserved as deprecated methods that internally delegate to v5 implementations. You can update the dependency version and existing code will compile and run without changes. This skill guides an AI agent through a full migration to the new v5 APIs.

## Migration Strategy

1. Update the dependency version
2. Add `CometChatCalls.login()` after authentication
3. Replace `CallSettings` / `CallSettingsBuilder` → `SessionSettings` / `SessionSettingsBuilder`
4. Replace `startSession()` → `joinSession()`
5. Replace static `CometChatCalls.*` actions → `CallSession` instance methods
6. Replace `CometChatCallsEventsListener` → 5 focused lifecycle-aware listeners
7. Remove manual `removeCallsEventListeners()` calls
8. Remove `setAuthToken()` from `CallLogRequest`
9. Clean up removed APIs (`switchToVideoCall`, `setAvatarMode`, `enableVideoTileDrag`, `enableVideoTileClick`, `setMainVideoContainerSetting`)

## Step 1 — Update Dependency

```groovy
// build.gradle.kts
-implementation("com.cometchat:calls-sdk-android:4.x.x")
+implementation("com.cometchat:calls-sdk-android:5.0.1")
```

If using CometChat UI Kits, just updating the version is sufficient — the UI Kit works through the deprecated compatibility layer.

## Step 2 — Add Authentication

v4 had no dedicated Calls SDK login. v5 introduces `CometChatCalls.login()`.

### v4 (remove this pattern)

```java
String authToken = CometChat.getUserAuthToken();
CometChatCalls.generateToken(sessionId, authToken, new CometChatCalls.CallbackListener<GenerateToken>() {
    @Override
    public void onSuccess(GenerateToken generateToken) {
        // Had to pass authToken manually
    }
    @Override
    public void onError(CometChatException e) { }
});
```

### v5 (replace with)

```java
// Call once after CometChat.login() succeeds
CometChatCalls.login(authToken, new CometChatCalls.CallbackListener<CallUser>() {
    @Override
    public void onSuccess(CallUser callUser) {
        // Auth token cached internally — no need to pass it around
    }
    @Override
    public void onError(CometChatException e) { }
});
```

## Step 3 — Replace Session Settings

`CallSettings` / `CallSettingsBuilder` → `SessionSettings` / `SessionSettingsBuilder`. Note the inverted boolean logic on visibility methods.

### v4 (remove this pattern)

```java
CallSettings callSettings = new CometChatCalls.CallSettingsBuilder(activity)
    .setIsAudioOnly(true)
    .setDefaultLayoutEnable(true)
    .showEndCallButton(true)
    .showMuteAudioButton(true)
    .showPauseVideoButton(true)
    .showSwitchCameraButton(true)
    .showAudioModeButton(true)
    .showRecordingButton(true)
    .startWithAudioMuted(false)
    .startWithVideoMuted(false)
    .setDefaultAudioMode("SPEAKER")
    .setMode("DEFAULT")
    .autoRecordOnCallStart(false)
    .build();
```

### v5 (replace with)

```java
SessionSettings sessionSettings = new CometChatCalls.SessionSettingsBuilder()
    .setSessionType(SessionType.VOICE)
    .startAudioMuted(false)
    .startVideoPaused(false)
    .setLayout(LayoutType.TILE)
    .setAudioMode(AudioMode.SPEAKER)
    .hideLeaveSessionButton(false)
    .hideToggleAudioButton(false)
    .hideToggleVideoButton(false)
    .hideSwitchCameraButton(false)
    .hideAudioModeButton(false)
    .hideRecordingButton(false)
    .hideControlPanel(false)
    .hideHeaderPanel(false)
    .enableAutoStartRecording(false)
    .build();
```

### Builder Method Mapping

| v4 Method | v5 Method | Notes |
|-----------|-----------|-------|
| `setIsAudioOnly(true)` | `setSessionType(SessionType.VOICE)` | Use `SessionType.VIDEO` for video |
| `setDefaultLayoutEnable(bool)` | `hideControlPanel(!bool)` + `hideHeaderPanel(!bool)` | Inverted logic |
| `showEndCallButton(bool)` | `hideLeaveSessionButton(!bool)` | Inverted logic |
| `showMuteAudioButton(bool)` | `hideToggleAudioButton(!bool)` | Inverted logic |
| `showPauseVideoButton(bool)` | `hideToggleVideoButton(!bool)` | Inverted logic |
| `showSwitchCameraButton(bool)` | `hideSwitchCameraButton(!bool)` | Inverted logic |
| `showAudioModeButton(bool)` | `hideAudioModeButton(!bool)` | Inverted logic |
| `showRecordingButton(bool)` | `hideRecordingButton(!bool)` | Inverted logic |
| `startWithAudioMuted(bool)` | `startAudioMuted(bool)` | Same logic |
| `startWithVideoMuted(bool)` | `startVideoPaused(bool)` | Same logic |
| `setDefaultAudioMode("SPEAKER")` | `setAudioMode(AudioMode.SPEAKER)` | Enum instead of string |
| `setMode("DEFAULT")` | `setLayout(LayoutType.TILE)` | Enum instead of string |
| `autoRecordOnCallStart(bool)` | `enableAutoStartRecording(bool)` | Same logic |
| `setAvatarMode(string)` | *Removed* | Delete usage |
| `setMainVideoContainerSetting(setting)` | *Removed* | Delete usage |
| `enableVideoTileDrag(bool)` | *Removed* | Delete usage |
| `enableVideoTileClick(bool)` | *Removed* | Delete usage |
| `setEventListener(listener)` | Use `CallSession` listeners | See Step 6 |

## Step 4 — Replace Join Session

`startSession()` → `joinSession()`. The v5 method returns a `CallSession` object.

### v4 (remove this pattern)

```java
CometChatCalls.generateToken(sessionId, authToken, new CometChatCalls.CallbackListener<GenerateToken>() {
    @Override
    public void onSuccess(GenerateToken generateToken) {
        CometChatCalls.startSession(generateToken.getToken(), callSettings, callView,
            new CometChatCalls.CallbackListener<String>() {
                @Override public void onSuccess(String s) { }
                @Override public void onError(CometChatException e) { }
            });
    }
    @Override
    public void onError(CometChatException e) { }
});
```

### v5 (replace with — recommended one-step)

```java
CometChatCalls.joinSession(sessionId, sessionSettings, callView,
    new CometChatCalls.CallbackListener<CallSession>() {
        @Override
        public void onSuccess(CallSession callSession) {
            // Token generation + join handled internally
            // Use callSession for actions and events
        }
        @Override
        public void onError(CometChatException e) { }
    });
```

Key differences:
- Returns `CallSession` object, not raw `String`
- Accepts `sessionId` directly — handles token generation internally
- No need to pass `authToken` (handled by `CometChatCalls.login()`)

## Step 5 — Replace Session Actions

Static `CometChatCalls.*` methods → instance methods on `CallSession`.

### Action Method Mapping

| v4 Static Method | v5 CallSession Method |
|---|---|
| `CometChatCalls.endSession()` | `callSession.leaveSession()` |
| `CometChatCalls.switchCamera()` | `callSession.switchCamera()` |
| `CometChatCalls.muteAudio(true)` | `callSession.muteAudio()` |
| `CometChatCalls.muteAudio(false)` | `callSession.unmuteAudio()` |
| `CometChatCalls.pauseVideo(true)` | `callSession.pauseVideo()` |
| `CometChatCalls.pauseVideo(false)` | `callSession.resumeVideo()` |
| `CometChatCalls.setAudioMode("SPEAKER")` | `callSession.setAudioMode(AudioMode.SPEAKER)` |
| `CometChatCalls.enterPIPMode()` | `callSession.enablePictureInPictureLayout()` |
| `CometChatCalls.exitPIPMode()` | `callSession.disablePictureInPictureLayout()` |
| `CometChatCalls.startRecording()` | `callSession.startRecording()` |
| `CometChatCalls.stopRecording()` | `callSession.stopRecording()` |
| `CometChatCalls.switchToVideoCall()` | *Removed — delete usage* |

Get the `CallSession` instance:

```java
CallSession callSession = CallSession.getInstance();
```

## Step 6 — Replace Event Listeners

This is the biggest change. Replace the single `CometChatCallsEventsListener` with 5 focused, lifecycle-aware listeners on `CallSession`. Pass your Activity/Fragment as the first parameter — listeners auto-remove on destroy.

### v4 (remove this pattern)

```java
CometChatCalls.addCallsEventListeners("LISTENER_ID", new CometChatCallsEventsListener() {
    @Override public void onCallEnded() { }
    @Override public void onCallEndButtonPressed() { }
    @Override public void onUserJoined(RTCUser user) { }
    @Override public void onUserLeft(RTCUser user) { }
    @Override public void onUserListChanged(ArrayList<RTCUser> users) { }
    @Override public void onAudioModeChanged(ArrayList<AudioMode> devices) { }
    @Override public void onCallSwitchedToVideo(CallSwitchRequestInfo info) { }
    @Override public void onUserMuted(RTCMutedUser muteObj) { }
    @Override public void onRecordingToggled(RTCRecordingInfo info) { }
    @Override public void onError(CometChatException e) { }
});

// Also remove this cleanup call — no longer needed
CometChatCalls.removeCallsEventListeners("LISTENER_ID");
```

### v5 (replace with)

```java
CallSession callSession = CallSession.getInstance();

// 1. Session lifecycle
callSession.addSessionStatusListener(this, new SessionStatusListener() {
    @Override public void onSessionJoined() { }
    @Override public void onSessionLeft() { }
    @Override public void onSessionTimedOut() { }
    @Override public void onConnectionLost() { }
    @Override public void onConnectionRestored() { }
    @Override public void onConnectionClosed() { }
});

// 2. Participant events
callSession.addParticipantEventListener(this, new ParticipantEventListener() {
    @Override public void onParticipantJoined(Participant participant) { }
    @Override public void onParticipantLeft(Participant participant) { }
    @Override public void onParticipantListChanged(List<Participant> participants) { }
    @Override public void onParticipantAudioMuted(Participant participant) { }
    @Override public void onParticipantAudioUnmuted(Participant participant) { }
    @Override public void onParticipantVideoPaused(Participant participant) { }
    @Override public void onParticipantVideoResumed(Participant participant) { }
    @Override public void onParticipantHandRaised(Participant participant) { }
    @Override public void onParticipantHandLowered(Participant participant) { }
    @Override public void onParticipantStartedScreenShare(Participant participant) { }
    @Override public void onParticipantStoppedScreenShare(Participant participant) { }
    @Override public void onParticipantStartedRecording(Participant participant) { }
    @Override public void onParticipantStoppedRecording(Participant participant) { }
    @Override public void onDominantSpeakerChanged(Participant participant) { }
});

// 3. Media state events
callSession.addMediaEventsListener(this, new MediaEventsListener() {
    @Override public void onAudioMuted() { }
    @Override public void onAudioUnMuted() { }
    @Override public void onVideoPaused() { }
    @Override public void onVideoResumed() { }
    @Override public void onRecordingStarted() { }
    @Override public void onRecordingStopped() { }
    @Override public void onAudioModeChanged(AudioMode audioMode) { }
    @Override public void onCameraFacingChanged(CameraFacing facing) { }
});

// 4. Button click events
callSession.addButtonClickListener(this, new ButtonClickListener() {
    @Override public void onLeaveSessionButtonClicked() { }
    @Override public void onToggleAudioButtonClicked() { }
    @Override public void onToggleVideoButtonClicked() { }
    @Override public void onSwitchCameraButtonClicked() { }
    @Override public void onRaiseHandButtonClicked() { }
    @Override public void onShareInviteButtonClicked() { }
    @Override public void onChangeLayoutButtonClicked() { }
    @Override public void onParticipantListButtonClicked() { }
    @Override public void onChatButtonClicked() { }
    @Override public void onRecordingToggleButtonClicked() { }
});

// 5. Layout events
callSession.addLayoutListener(this, new LayoutListener() {
    @Override public void onCallLayoutChanged(LayoutType layoutType) { }
    @Override public void onParticipantListVisible() { }
    @Override public void onParticipantListHidden() { }
    @Override public void onPictureInPictureLayoutEnabled() { }
    @Override public void onPictureInPictureLayoutDisabled() { }
});
```

### Event Mapping

| v4 Event | v5 Listener | v5 Event |
|----------|-------------|----------|
| `onCallEnded()` | `SessionStatusListener` | `onSessionLeft()` |
| `onCallEndButtonPressed()` | `ButtonClickListener` | `onLeaveSessionButtonClicked()` |
| `onSessionTimeout()` | `SessionStatusListener` | `onSessionTimedOut()` |
| `onUserJoined(RTCUser)` | `ParticipantEventListener` | `onParticipantJoined(Participant)` |
| `onUserLeft(RTCUser)` | `ParticipantEventListener` | `onParticipantLeft(Participant)` |
| `onUserListChanged(ArrayList<RTCUser>)` | `ParticipantEventListener` | `onParticipantListChanged(List<Participant>)` |
| `onAudioModeChanged(ArrayList<AudioMode>)` | `MediaEventsListener` | `onAudioModeChanged(AudioMode)` |
| `onCallSwitchedToVideo(CallSwitchRequestInfo)` | *Removed* | Delete handler |
| `onUserMuted(RTCMutedUser)` | `ParticipantEventListener` | `onParticipantAudioMuted(Participant)` |
| `onRecordingToggled(RTCRecordingInfo)` | `MediaEventsListener` | `onRecordingStarted()` / `onRecordingStopped()` |
| `onError(CometChatException)` | — | Errors via `CallbackListener.onError()` |

## Step 7 — Update Call Logs

Remove `setAuthToken()` from `CallLogRequest` — auth is handled by `CometChatCalls.login()`.

### v4

```java
CallLogRequest request = new CallLogRequest.CallLogRequestBuilder()
    .setAuthToken(authToken)
    .setLimit(30)
    .build();
```

### v5

```java
CallLogRequest request = new CallLogRequest.CallLogRequestBuilder()
    .setLimit(30)
    .build();
```

## Deprecated Classes Reference

| Deprecated Class | v5 Replacement |
|-----------------|----------------|
| `CallSettings` | `SessionSettings` |
| `CometChatCalls.CallSettingsBuilder` | `CometChatCalls.SessionSettingsBuilder` |
| `CometChatCallsEventsListener` | 5 focused listeners on `CallSession` |
| `RTCRecordingInfo` | `MediaEventsListener` callbacks |
| `CallSwitchRequestInfo` | *Removed (no replacement)* |
| `RTCUser` | `Participant` |
| `RTCMutedUser` | `Participant` (via `onParticipantAudioMuted`) |

## Key Imports (v5)

```java
import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.calls.core.CallAppSettings;
import com.cometchat.calls.core.CallSession;
import com.cometchat.calls.core.SessionSettings;
import com.cometchat.calls.core.CallLogRequest;
import com.cometchat.calls.model.Participant;
import com.cometchat.calls.model.GenerateToken;
import com.cometchat.calls.model.CallUser;
import com.cometchat.calls.model.AudioMode;
import com.cometchat.calls.model.SessionType;
import com.cometchat.calls.model.LayoutType;
import com.cometchat.calls.model.CameraFacing;
import com.cometchat.calls.listeners.SessionStatusListener;
import com.cometchat.calls.listeners.ParticipantEventListener;
import com.cometchat.calls.listeners.MediaEventsListener;
import com.cometchat.calls.listeners.ButtonClickListener;
import com.cometchat.calls.listeners.LayoutListener;
import com.cometchat.calls.exceptions.CometChatException;
```

## Gotchas

- v5 is a drop-in replacement — you can update the dependency and migrate incrementally; v4 APIs still work but are deprecated
- Boolean logic is **inverted** for visibility methods: `showX(true)` → `hideX(false)`
- `muteAudio(true/false)` is now two separate methods: `muteAudio()` and `unmuteAudio()`
- `pauseVideo(true/false)` is now two separate methods: `pauseVideo()` and `resumeVideo()`
- `onAudioModeChanged` in v4 received a list of devices; in v5 it receives a single `AudioMode` enum
- `onRecordingToggled` is split into `onRecordingStarted()` and `onRecordingStopped()`
- `onCallSwitchedToVideo` and `switchToVideoCall()` are removed with no replacement
- Lifecycle-aware listeners auto-cleanup — delete all `removeCallsEventListeners()` calls
- `generateToken()` no longer requires `authToken` parameter — it's cached from `login()`
