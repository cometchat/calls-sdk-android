package com.cometchat.samplecallsringing.calls

import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.core.SessionSettings
import com.cometchat.calls.model.AudioMode
import com.cometchat.calls.model.LayoutType
import com.cometchat.calls.model.SessionType

/**
 * The session settings for a call. `initiateCall` freezes them on the session it prepares, and the
 * `joinSession` that follows ignores the ones it is given, so the caller builds both from here.
 */
object CallSettings {

    fun make(title: String, isVideo: Boolean, isPeerCall: Boolean): SessionSettings =
        CometChatCalls.SessionSettingsBuilder()
            .setTitle(title)
            .setSessionType(if (isVideo) SessionType.VIDEO else SessionType.VOICE)
            .setLayout(if (isVideo) LayoutType.TILE else LayoutType.SPOTLIGHT)
            .setAudioMode(if (isVideo) AudioMode.SPEAKER else AudioMode.EARPIECE)
            .startVideoPaused(!isVideo)
            .startAudioMuted(false)
            // 1:1: the call ends for both when either leaves. A group call carries on.
            .setPeerCall(isPeerCall)
            .build()
}
