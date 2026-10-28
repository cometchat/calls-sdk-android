package com.cometchat.samplecallsringing.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cometchat.samplecallsringing.ui.fragments.CallLogsFragment
import com.cometchat.samplecallsringing.ui.fragments.GroupsFragment
import com.cometchat.samplecallsringing.ui.fragments.UsersFragment

/** Users, Groups and History, in the order of the bottom navigation. */
class HomePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> UsersFragment()
        1 -> GroupsFragment()
        2 -> CallLogsFragment()
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
}
