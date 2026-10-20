package com.cometchat.samplecallsringing.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.databinding.ItemUserSelectBinding

/** Users with a checkbox, for picking the members of a new group. Selection survives searching. */
class SelectUsersAdapter(
    private val maxSelected: Int,
    private val onLimitReached: () -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<SelectUsersAdapter.ViewHolder>() {

    private val users = mutableListOf<User>()

    /** Picked users by UID, in the order they were picked. */
    val selected = linkedMapOf<String, User>()

    inner class ViewHolder(val binding: ItemUserSelectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemUserSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val name = user.name ?: user.uid
        with(holder.binding) {
            ivAvatar.set(name, user.avatar)
            tvName.text = name
            checkbox.isChecked = user.uid in selected
            root.setOnClickListener {
                when {
                    user.uid in selected -> selected.remove(user.uid)
                    selected.size >= maxSelected -> {
                        onLimitReached()
                        return@setOnClickListener
                    }
                    else -> selected[user.uid] = user
                }
                notifyItemChanged(holder.bindingAdapterPosition)
                onSelectionChanged()
            }
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
