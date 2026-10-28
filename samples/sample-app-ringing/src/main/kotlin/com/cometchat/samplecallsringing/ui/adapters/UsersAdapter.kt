package com.cometchat.samplecallsringing.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.ItemUserBinding

class UsersAdapter(
    private val onVoiceCallClick: (User) -> Unit,
    private val onVideoCallClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private val users = mutableListOf<User>()

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder =
        UserViewHolder(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val context = holder.itemView.context
        val name = user.name ?: user.uid
        val online = user.status == CometChatConstants.USER_STATUS_ONLINE
        with(holder.binding) {
            ivAvatar.set(name, user.avatar, isOnline = online)
            tvName.text = name
            tvStatus.setText(if (online) R.string.status_online else R.string.status_offline)
            tvStatus.setTextColor(ContextCompat.getColor(context, if (online) R.color.status_online else R.color.text_secondary))
            btnAudioCall.contentDescription = context.getString(R.string.voice_call_to, name)
            btnVideoCall.contentDescription = context.getString(R.string.video_call_to, name)
            btnAudioCall.setOnClickListener { onVoiceCallClick(user) }
            btnVideoCall.setOnClickListener { onVideoCallClick(user) }
        }
    }

    override fun getItemCount(): Int = users.size

    fun submit(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    fun append(list: List<User>) {
        val start = users.size
        users.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }
}
