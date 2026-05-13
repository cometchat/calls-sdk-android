package com.cometchat.samplecallsvoip.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cometchat.samplecallsvoip.R
import com.cometchat.samplecallsvoip.data.repository.SampleUser
import com.google.android.material.imageview.ShapeableImageView

class SampleUsersAdapter(
    private val userList: MutableList<SampleUser>,
    private val onUserSelected: (SampleUser) -> Unit
) : RecyclerView.Adapter<SampleUsersAdapter.UserViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    fun updateList(users: List<SampleUser>) {
        userList.clear()
        userList.addAll(users)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sample_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user, position == selectedPosition)

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = if (selectedPosition == position) {
                RecyclerView.NO_POSITION
            } else {
                position
            }
            notifyItemChanged(previousPosition)
            notifyItemChanged(position)
            onUserSelected(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardUser: CardView = itemView.findViewById(R.id.card_user)
        private val avatar: ShapeableImageView = itemView.findViewById(R.id.avatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserUid: TextView = itemView.findViewById(R.id.tv_user_uid)
        private val ivSelected: ImageView = itemView.findViewById(R.id.iv_selected)

        fun bind(user: SampleUser, isSelected: Boolean) {
            tvUserName.text = user.name
            tvUserUid.text = user.uid

            if (user.avatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(avatar)
            } else {
                avatar.setImageResource(R.drawable.ic_default_avatar)
            }

            val card = cardUser as com.google.android.material.card.MaterialCardView
            if (isSelected) {
                ivSelected.visibility = View.VISIBLE
                card.strokeColor = ContextCompat.getColor(itemView.context, R.color.primary)
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary_light))
            } else {
                ivSelected.visibility = View.GONE
                card.strokeColor = ContextCompat.getColor(itemView.context, R.color.stroke_default)
                card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.background))
            }
        }
    }
}
