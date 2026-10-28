package com.cometchat.samplecallsringing.calls

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.CallGroup
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.samplecallsringing.R
import java.util.Date

/** How a call log reads in the History list and on the details screen. */
object CallLogDisplay {

    data class Status(@StringRes val label: Int, @ColorRes val color: Int, @DrawableRes val chip: Int)

    @StringRes
    fun kind(log: CallLog): Int = when {
        !log.callCategory.equals("call", ignoreCase = true) -> R.string.kind_meeting
        isGroup(log) -> R.string.kind_group_call
        isVoice(log) -> R.string.kind_voice_call
        else -> R.string.kind_video_call
    }

    fun isGroup(log: CallLog): Boolean =
        log.receiverType.equals(Constants.RECEIVER_TYPE_GROUP, ignoreCase = true) || log.receiver is CallGroup

    fun isVoice(log: CallLog): Boolean =
        log.type?.lowercase() in setOf(Constants.SESSION_TYPE_AUDIO, "voice")

    fun status(log: CallLog): Status = when (log.status?.lowercase()) {
        Constants.CALL_STATUS_ONGOING -> Status(R.string.status_live, R.color.status_online, R.drawable.bg_chip_success)
        Constants.CALL_STATUS_ENDED -> Status(R.string.status_ended, R.color.text_secondary, R.drawable.bg_chip_neutral)
        Constants.CALL_STATUS_CANCELLED -> Status(R.string.status_cancelled, R.color.error, R.drawable.bg_chip_error)
        Constants.CALL_STATUS_UNANSWERED -> Status(R.string.status_no_answer, R.color.error, R.drawable.bg_chip_error)
        Constants.CALL_STATUS_MISSED -> Status(R.string.status_missed, R.color.error, R.drawable.bg_chip_error)
        Constants.CALL_STATUS_REJECTED -> Status(R.string.status_declined, R.color.error, R.drawable.bg_chip_error)
        Constants.CALL_STATUS_BUSY -> Status(R.string.status_busy, R.color.error, R.drawable.bg_chip_error)
        Constants.CALL_STATUS_INITIATED -> Status(R.string.status_not_connected, R.color.error, R.drawable.bg_chip_error)
        else -> Status(R.string.unknown, R.color.text_secondary, R.drawable.bg_chip_neutral)
    }

    /**
     * The other side of the call: the group for a group call, otherwise whichever of the initiator
     * and receiver is not the signed-in user.
     */
    fun counterpart(log: CallLog): CallTarget? {
        (log.receiver as? CallGroup)?.let { group ->
            // A group log can come back without its guid; that row falls back to the users below.
            val guid: String? = group.guid
            if (!guid.isNullOrBlank()) return CallTarget(guid, group.name ?: guid, group.icon, isGroup = true)
        }
        val me = CometChatCalls.getLoggedInUser()?.uid
        return listOf(log.initiator, log.receiver)
            .filterIsInstance<CallUser>()
            .firstOrNull { !it.uid.isNullOrEmpty() && it.uid != me }
            ?.let { CallTarget(it.uid, it.name ?: it.uid, it.avatar, isGroup = false) }
    }

    fun title(context: Context, log: CallLog): String =
        counterpart(log)?.name ?: context.getString(R.string.unknown)

    /** "Today", "Yesterday" or a short date, for a time in seconds. */
    fun date(context: Context, seconds: Long): String {
        if (seconds <= 0) return context.getString(R.string.none_value)
        val millis = seconds * 1000
        return when {
            DateUtils.isToday(millis) -> context.getString(R.string.today)
            DateUtils.isToday(millis + DateUtils.DAY_IN_MILLIS) -> context.getString(R.string.yesterday)
            else -> DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
        }
    }

    fun time(context: Context, seconds: Long): String =
        if (seconds <= 0) context.getString(R.string.none_value)
        else DateFormat.getTimeFormat(context).format(Date(seconds * 1000))

    fun dateTime(context: Context, seconds: Long): String =
        if (seconds <= 0) context.getString(R.string.none_value)
        else context.getString(R.string.date_time, date(context, seconds), time(context, seconds))

    /** "m:ss" or "h:mm:ss"; a dash for a call that never connected. */
    fun duration(context: Context, minutes: Double): String {
        val total = Math.round(minutes * 60).toInt()
        if (total <= 0) return context.getString(R.string.none_value)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
