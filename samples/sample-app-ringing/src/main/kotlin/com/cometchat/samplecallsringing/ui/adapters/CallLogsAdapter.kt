package com.cometchat.samplecallsringing.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.model.CallLog
import com.cometchat.samplecallsringing.calls.CallLogDisplay
import com.cometchat.samplecallsringing.databinding.ItemCallLogBinding

/** One row per call: who, voice/video/group, the outcome, when, and how long. */
class CallLogsAdapter(
    private val onClick: (CallLog) -> Unit
) : RecyclerView.Adapter<CallLogsAdapter.CallLogViewHolder>() {

    private val callLogs = mutableListOf<CallLog>()

    inner class CallLogViewHolder(val binding: ItemCallLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder =
        CallLogViewHolder(ItemCallLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val log = callLogs[position]
        val context = holder.itemView.context
        val title = CallLogDisplay.title(context, log)
        val counterpart = CallLogDisplay.counterpart(log)
        val status = CallLogDisplay.status(log)
        val statusColor = ContextCompat.getColor(context, status.color)

        with(holder.binding) {
            ivAvatar.set(title, counterpart?.avatar, initialsCount = if (counterpart?.isGroup == true) 2 else 1)
            tvName.text = title
            tvKind.setText(CallLogDisplay.kind(log))
            tvCallStatus.setText(status.label)
            tvCallStatus.setTextColor(statusColor)
            TextViewCompat.setCompoundDrawableTintList(tvCallStatus, ColorStateList.valueOf(statusColor))
            tvTimestamp.text = CallLogDisplay.dateTime(context, log.initiatedAt)
            tvDuration.text = CallLogDisplay.duration(context, log.totalDurationInMinutes)
            root.setOnClickListener { onClick(log) }
        }
    }

    override fun getItemCount(): Int = callLogs.size

    fun submit(logs: List<CallLog>) {
        callLogs.clear()
        callLogs.addAll(logs)
        notifyDataSetChanged()
    }

    fun append(logs: List<CallLog>) {
        val start = callLogs.size
        callLogs.addAll(logs)
        notifyItemRangeInserted(start, logs.size)
    }
}
