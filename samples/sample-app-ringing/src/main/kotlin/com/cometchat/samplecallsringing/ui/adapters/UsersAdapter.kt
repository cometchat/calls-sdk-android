package com.cometchat.samplecallsringing.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.ItemUserBinding

class UsersAdapter(
    private val userList: MutableList<User>,
    private val onVoiceCallClick: (User) -> Unit,
    private val onVideoCallClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        with(holder.binding) {
            tvName.text = user.name
            tvStatus.text = user.status

            Glide.with(ivAvatar.context)
                .load(user.avatar)
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(ivAvatar)

            btnAudioCall.setOnClickListener { onVoiceCallClick(user) }
            btnVideoCall.setOnClickListener { onVideoCallClick(user) }
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(users: List<User>) {
        userList.clear()
        userList.addAll(users)
        notifyDataSetChanged()
    }

    fun appendList(users: List<User>) {
        val start = userList.size
        userList.addAll(users)
        notifyItemRangeInserted(start, users.size)
    }
}
