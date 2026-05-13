package com.cometchat.samplecallsvoip.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.samplecallsvoip.R
import com.cometchat.samplecallsvoip.databinding.ItemCallLogBinding

class CallLogsAdapter(
    private val callLogs: MutableList<CallLog>
) : RecyclerView.Adapter<CallLogsAdapter.CallLogViewHolder>() {

    inner class CallLogViewHolder(val binding: ItemCallLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val binding = ItemCallLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val callLog = callLogs[position]

        with(holder.binding) {
            // Call type icon
            val isVideo = callLog.type == "video"
            ivCallType.setImageResource(
                if (isVideo) R.drawable.ic_call_video_log else R.drawable.ic_call_audio_log
            )

            // Caller/receiver name
            val loggedInUid = CometChatCalls.getLoggedInUser()?.uid
            val initiator = callLog.initiator as? CallUser
            val receiver = callLog.receiver as? CallUser
            val isOutgoing = initiator?.uid == loggedInUid
            val displayName = if (isOutgoing) {
                receiver?.name ?: holder.itemView.context.getString(R.string.unknown)
            } else {
                initiator?.name ?: holder.itemView.context.getString(R.string.unknown)
            }
            tvName.text = displayName

            // Call status
            tvCallStatus.text = formatCallStatus(callLog.status, isOutgoing)

            // Timestamp
            val timestamp = callLog.initiatedAt
            if (timestamp > 0) {
                tvTimestamp.text = DateUtils.getRelativeTimeSpanString(
                    timestamp * 1000,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
            } else {
                tvTimestamp.text = ""
            }
        }
    }

    override fun getItemCount(): Int = callLogs.size

    fun updateList(logs: List<CallLog>) {
        callLogs.clear()
        callLogs.addAll(logs)
        notifyDataSetChanged()
    }

    private fun formatCallStatus(status: String?, isOutgoing: Boolean): String {
        return when (status) {
            "initiated" -> if (isOutgoing) "Outgoing" else "Incoming"
            "ongoing" -> "Ongoing"
            "unanswered" -> "Missed"
            "rejected" -> "Rejected"
            "cancelled" -> "Cancelled"
            "busy" -> "Busy"
            "ended" -> "Ended"
            else -> status ?: "Unknown"
        }
    }
}
