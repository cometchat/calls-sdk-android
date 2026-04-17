package com.cometchat.samplecallsringing.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.core.Call
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.ItemCallLogBinding

class CallLogsAdapter(
    private val callLogs: MutableList<BaseMessage>
) : RecyclerView.Adapter<CallLogsAdapter.CallLogViewHolder>() {

    inner class CallLogViewHolder(val binding: ItemCallLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val binding = ItemCallLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val message = callLogs[position]
        if (message !is Call) return

        with(holder.binding) {
            // Call type icon
            val isVideo = message.type == CometChatConstants.CALL_TYPE_VIDEO
            ivCallType.setImageResource(
                if (isVideo) R.drawable.ic_call_video_log else R.drawable.ic_call_audio_log
            )

            // Caller/receiver name
            val loggedInUid = CometChat.getLoggedInUser()?.uid
            val isOutgoing = message.sender?.uid == loggedInUid
            val displayName = if (isOutgoing) {
                (message.callReceiver as? com.cometchat.chat.models.User)?.name ?: holder.itemView.context.getString(R.string.unknown)
            } else {
                message.sender?.name ?: holder.itemView.context.getString(R.string.unknown)
            }
            tvName.text = displayName

            // Call status
            tvCallStatus.text = formatCallStatus(message.callStatus, isOutgoing)

            // Timestamp
            tvTimestamp.text = DateUtils.getRelativeTimeSpanString(
                message.sentAt * 1000,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
        }
    }

    override fun getItemCount(): Int = callLogs.size

    fun updateList(logs: List<BaseMessage>) {
        callLogs.clear()
        callLogs.addAll(logs)
        notifyDataSetChanged()
    }

    private fun formatCallStatus(status: String?, isOutgoing: Boolean): String {
        return when (status) {
            CometChatConstants.CALL_STATUS_INITIATED -> if (isOutgoing) "Outgoing" else "Incoming"
            CometChatConstants.CALL_STATUS_ONGOING -> "Ongoing"
            CometChatConstants.CALL_STATUS_UNANSWERED -> "Missed"
            CometChatConstants.CALL_STATUS_REJECTED -> "Rejected"
            CometChatConstants.CALL_STATUS_CANCELLED -> "Cancelled"
            CometChatConstants.CALL_STATUS_BUSY -> "Busy"
            CometChatConstants.CALL_STATUS_ENDED -> "Ended"
            else -> status ?: "Unknown"
        }
    }
}
