package com.cometchat.samplecallsringing.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.models.Group
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.ItemGroupBinding

class GroupsAdapter(
    private val onVoiceCallClick: (Group) -> Unit,
    private val onVideoCallClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    private val groups = mutableListOf<Group>()

    inner class GroupViewHolder(val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder =
        GroupViewHolder(ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        val context = holder.itemView.context
        val name = group.name ?: group.guid
        with(holder.binding) {
            ivAvatar.set(name, group.icon, initialsCount = 2)
            tvName.text = name
            tvMembers.text = context.resources.getQuantityString(R.plurals.member_count, group.membersCount, group.membersCount)
            btnAudioCall.contentDescription = context.getString(R.string.voice_call_to, name)
            btnVideoCall.contentDescription = context.getString(R.string.video_call_to, name)
            btnAudioCall.setOnClickListener { onVoiceCallClick(group) }
            btnVideoCall.setOnClickListener { onVideoCallClick(group) }
        }
    }

    override fun getItemCount(): Int = groups.size

    fun submit(list: List<Group>) {
        groups.clear()
        groups.addAll(list)
        notifyDataSetChanged()
    }

    fun append(list: List<Group>) {
        val start = groups.size
        groups.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }
}
