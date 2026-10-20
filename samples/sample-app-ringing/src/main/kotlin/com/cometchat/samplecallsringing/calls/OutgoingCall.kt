package com.cometchat.samplecallsringing.calls

import com.cometchat.calls.constants.Constants

/**
 * State of one outgoing call, from the tap on a call button until the call is joined or over.
 *
 * Plain Kotlin with no Android or SDK calls: [com.cometchat.samplecallsringing.ui.activity.OutgoingCallActivity]
 * feeds it SDK callbacks and user actions and renders whatever it says. Every input returns `true`
 * only when it moved the state, so a duplicate SDK event or a double-tapped button changes nothing
 * and the activity can key its side effects on the return value.
 *
 * ```
 * Initiating ──initiated(id)──▶ Ringing ──participantJoined──▶ Joined
 *     │                           │
 *     └──initiateFailed──▶ Ended(FAILED)
 *                                 ├──cancel─────────────────▶ Ended(CANCELLED)
 *                                 ├──timeout────────────────▶ Ended(TIMEOUT)
 *                                 ├──serverEnded(status)────▶ Ended(DECLINED / BUSY / CLOSED)
 *                                 └──closedWithServerStatus─▶ Ended(DECLINED / BUSY / CLOSED / LOST)
 * ```
 */
class OutgoingCall(
    /**
     * A group call: the first member to answer is the acceptance, and the caller giving up
     * withdraws the call (`cancelled`) rather than calling it unanswered, which is each member's
     * own outcome.
     */
    val isGroup: Boolean = false
) {

    sealed class State {
        /** `initiateCall` is in flight; there is no session ID yet. */
        object Initiating : State() {
            override fun toString(): String = "Initiating"
        }

        /** The call exists and the callee's device is ringing. [remoteConnected] flips on `onParticipantConnected`. */
        data class Ringing(
            val sessionId: String,
            val remoteConnected: Boolean = false,
            /** Distinct users whose device prepared the session; a group's status line counts them. */
            val connectedCount: Int = 0
        ) : State()

        /** The callee answered; the call screen joins the session. */
        data class Joined(val sessionId: String) : State()

        /** Terminal. [sessionId] is null when the call never got past [Initiating]. */
        data class Ended(val sessionId: String?, val reason: EndReason) : State()
    }

    /**
     * Why the call ended. [CLOSED] is the far side ending it; [LOST] is the prepared session
     * closing before any callee device connected, which is this device's own connection failing.
     */
    enum class EndReason { CANCELLED, TIMEOUT, FAILED, CLOSED, LOST, DECLINED, BUSY }

    /** What the status line says. The activity maps these to string resources. */
    enum class StatusText { CALLING, RINGING, NO_ANSWER, FAILED, ENDED, DECLINED, BUSY, NONE }

    var state: State = State.Initiating
        private set

    /** Called after each accepted input, once [state] holds the new value. */
    var onTransition: ((from: State, to: State) -> Unit)? = null

    val sessionId: String?
        get() = when (val s = state) {
            State.Initiating -> null
            is State.Ringing -> s.sessionId
            is State.Joined -> s.sessionId
            is State.Ended -> s.sessionId
        }

    /** The end button works only while ringing: before that there is nothing to cancel. */
    val canCancel: Boolean get() = state is State.Ringing

    val isRinging: Boolean get() = state is State.Ringing
    val isJoined: Boolean get() = state is State.Joined
    val isEnded: Boolean get() = state is State.Ended

    /** Members ringing so far; 0 until the first device prepared the session. */
    val connectedCount: Int get() = (state as? State.Ringing)?.connectedCount ?: 0

    val statusText: StatusText
        get() = when (val s = state) {
            State.Initiating -> StatusText.CALLING
            is State.Ringing -> if (s.remoteConnected) StatusText.RINGING else StatusText.CALLING
            is State.Joined -> StatusText.NONE
            is State.Ended -> when (s.reason) {
                EndReason.CANCELLED -> StatusText.NONE
                EndReason.TIMEOUT -> StatusText.NO_ANSWER
                EndReason.FAILED, EndReason.LOST -> StatusText.FAILED
                EndReason.CLOSED -> StatusText.ENDED
                EndReason.DECLINED -> StatusText.DECLINED
                EndReason.BUSY -> StatusText.BUSY
            }
        }

    /** `initiateCall` succeeded. */
    fun initiated(sessionId: String): Boolean {
        if (state !is State.Initiating) return false
        transition(State.Ringing(sessionId))
        return true
    }

    /** `initiateCall` failed: nothing exists on the server, so the call is over. */
    fun initiateFailed(): Boolean {
        if (state !is State.Initiating) return false
        transition(State.Ended(null, EndReason.FAILED))
        return true
    }

    private val connectedUids = mutableSetOf<String>()

    /**
     * A callee's device prepared the session (`onParticipantConnected`): "Calling…" becomes
     * "Ringing…". [uid] keeps one user ringing on several devices from counting twice.
     */
    fun participantConnected(uid: String?): Boolean {
        val s = state as? State.Ringing ?: return false
        if (!connectedUids.add(uid ?: "")) return false
        transition(s.copy(remoteConnected = true, connectedCount = connectedUids.size))
        return true
    }

    /** The callee answered (`onParticipantJoined`). */
    fun participantJoined(): Boolean {
        val s = state as? State.Ringing ?: return false
        transition(State.Joined(s.sessionId))
        return true
    }

    /**
     * The server says the call is over, read while it still rings here. Nothing pushes a decline
     * to the caller, so the activity polls the call log. `initiated`, `ongoing` and an unreadable
     * status keep the call ringing: a failed request must never end a live call.
     */
    fun serverEnded(serverStatus: String?): Boolean {
        val s = state as? State.Ringing ?: return false
        val status = serverStatus?.lowercase() ?: return false
        if (status == Constants.CALL_STATUS_INITIATED || status == Constants.CALL_STATUS_ONGOING) return false
        transition(State.Ended(s.sessionId, reasonFor(status) ?: EndReason.CLOSED))
        return true
    }

    /**
     * The prepared session closed while ringing (`onConnectionClosed`). The close carries no
     * reason, so the activity reads the call's status from the server first: `rejected` / `busy`
     * end it as declined / busy. Otherwise, once a callee's device had connected, the far side
     * ended it ([EndReason.CLOSED]); before that, this device's own connection failed
     * ([EndReason.LOST]) and the callee may still be ringing, so the caller must end the call
     * ([needsServerEnd]).
     */
    fun closedWithServerStatus(serverStatus: String?): Boolean {
        val s = state as? State.Ringing ?: return false
        val reason = reasonFor(serverStatus?.lowercase())
            ?: if (s.remoteConnected) EndReason.CLOSED else EndReason.LOST
        transition(State.Ended(s.sessionId, reason))
        return true
    }

    /** Only a lost connection needs this device to end the call on the server. */
    val needsServerEnd: Boolean
        get() = (state as? State.Ended)?.reason == EndReason.LOST

    /** The user tapped the end button. */
    fun cancel(): Boolean {
        val s = state as? State.Ringing ?: return false
        transition(State.Ended(s.sessionId, EndReason.CANCELLED))
        return true
    }

    /** The caller's ring timer expired. */
    fun timeout(): Boolean {
        val s = state as? State.Ringing ?: return false
        transition(State.Ended(s.sessionId, EndReason.TIMEOUT))
        return true
    }

    /**
     * The `endCall` status for this side giving up: no answer on a 1:1 call is `unanswered`;
     * everything else, and any group call, is `cancelled`.
     */
    fun endCallStatusFor(reason: EndReason): String =
        if (reason == EndReason.TIMEOUT && !isGroup) Constants.CALL_STATUS_UNANSWERED
        else Constants.CALL_STATUS_CANCELLED

    private fun reasonFor(status: String?): EndReason? = when (status) {
        Constants.CALL_STATUS_REJECTED -> EndReason.DECLINED
        Constants.CALL_STATUS_BUSY -> EndReason.BUSY
        else -> null
    }

    private fun transition(to: State) {
        val from = state
        state = to
        onTransition?.invoke(from, to)
    }

    companion object {
        /**
         * How long the caller waits for an answer. Shorter than the callee's ring
         * ([com.cometchat.samplecallsringing.push.PushNotifications.RING_TIMEOUT_MS], 60 s), so the
         * callee never keeps ringing for a call the caller has stopped waiting on.
         */
        const val RING_TIMEOUT_MS = 45_000L
    }
}
