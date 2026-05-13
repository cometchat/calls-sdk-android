package com.cometchat.samplecallsvoip.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cometchat.samplecallsvoip.ui.fragments.CallLogsFragment
import com.cometchat.samplecallsvoip.ui.fragments.UsersFragment

class HomePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> UsersFragment()
        1 -> CallLogsFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}
