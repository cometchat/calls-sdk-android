package com.cometchat.samplecallsringing.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.model.CallLog
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.calls.CallLauncher
import com.cometchat.samplecallsringing.calls.CallLogDisplay
import com.cometchat.samplecallsringing.databinding.ActivityCallDetailsBinding
import com.cometchat.samplecallsringing.databinding.ItemDetailFieldBinding
import com.cometchat.samplecallsringing.databinding.ItemParticipantBinding

/** One call from History: its outcome and timings, the participants, and calling back. */
class CallDetailsActivity : AppCompatActivity() {

    companion object {
        /** `CallLog` is not Parcelable, so the tapped log is handed over here. */
        private var pending: CallLog? = null

        fun start(context: Context, log: CallLog) {
            pending = log
            context.startActivity(Intent(context, CallDetailsActivity::class.java))
        }
    }

    private lateinit var binding: ActivityCallDetailsBinding
    private var log: CallLog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Gone after the process was recreated: nothing to show.
        val log = pending ?: run {
            finish()
            return
        }
        this.log = log
        binding = ActivityCallDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.toolbar.toolbar.setTitle(R.string.call_details)
        binding.toolbar.toolbar.setNavigationOnClickListener { finish() }
        bind(log)
    }

    private fun bind(log: CallLog) {
        val title = CallLogDisplay.title(this, log)
        val counterpart = CallLogDisplay.counterpart(log)
        val isVoice = CallLogDisplay.isVoice(log)

        binding.ivAvatar.set(title, counterpart?.avatar, initialsCount = if (counterpart?.isGroup == true) 2 else 1)
        binding.tvTitle.text = title
        binding.tvKind.setText(CallLogDisplay.kind(log))

        val status = CallLogDisplay.status(log)
        binding.tvStatus.setText(status.label)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, status.color))
        binding.tvStatus.setBackgroundResource(status.chip)

        val started = if (log.initiatedAt > 0) log.initiatedAt else 0L
        field(binding.fieldDuration, R.string.field_duration, CallLogDisplay.duration(this, log.totalDurationInMinutes))
        field(binding.fieldStarted, R.string.field_started, CallLogDisplay.dateTime(this, started))
        field(binding.fieldEnded, R.string.field_ended, CallLogDisplay.time(this, log.endedAt))
        field(binding.fieldType, R.string.field_type, getString(CallLogDisplay.kind(log)))
        val participants = log.participants.orEmpty()
        field(binding.fieldParticipants, R.string.field_participants, participants.size.toString())

        if (counterpart != null) {
            binding.btnCallBack.setText(
                when {
                    counterpart.isGroup && isVoice -> R.string.ring_group_voice
                    counterpart.isGroup -> R.string.ring_group_video
                    isVoice -> R.string.call_back_voice
                    else -> R.string.call_back_video
                }
            )
            binding.btnCallBack.setIconResource(if (isVoice) R.drawable.ic_call_white else R.drawable.ic_videocam_white)
            binding.btnCallBack.setOnClickListener { CallLauncher.start(this, counterpart, isVideo = !isVoice) }
        } else {
            binding.btnCallBack.visibility = View.GONE
        }

        binding.tvSessionId.text = log.sessionID

        binding.tvParticipantsLabel.visibility = if (participants.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutParticipants.visibility = if (participants.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutParticipants.dividerDrawable = ContextCompat.getDrawable(this, R.drawable.divider_inset)
        participants.forEach { participant ->
            val row = ItemParticipantBinding.inflate(layoutInflater, binding.layoutParticipants, true)
            val name = participant.name ?: participant.uid ?: getString(R.string.unknown)
            row.ivAvatar.set(name, participant.avatar)
            row.tvName.text = name
            val state = participant.state.orEmpty()
            row.tvState.text = state.replaceFirstChar { it.uppercase() }
            row.tvState.visibility = if (state.isEmpty()) View.GONE else View.VISIBLE
            row.tvDuration.text = CallLogDisplay.duration(this, participant.totalDurationInMinutes)
        }
    }

    private fun field(field: ItemDetailFieldBinding, label: Int, value: String) {
        field.tvLabel.setText(label)
        field.tvValue.text = value
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && pending === log) pending = null
    }
}
