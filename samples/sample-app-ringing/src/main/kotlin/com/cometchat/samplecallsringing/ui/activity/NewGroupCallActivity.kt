package com.cometchat.samplecallsringing.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.calls.CallLauncher
import com.cometchat.samplecallsringing.calls.CallTarget
import com.cometchat.samplecallsringing.databinding.ActivityNewGroupCallBinding
import com.cometchat.samplecallsringing.ui.adapters.SelectUsersAdapter
import com.cometchat.samplecallsringing.ui.fragments.SearchDebouncer
import java.util.UUID

/**
 * Creates a private group with the people you pick, then rings it. The group stays in the Groups
 * tab, so the same people can be rung again with one tap.
 */
class NewGroupCallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NewGroupCallActivity"
        private const val PAGE_SIZE = 30
        /** Every member's phone rings, so keep the group small. */
        private const val MAX_MEMBERS = 20
    }

    private lateinit var binding: ActivityNewGroupCallBinding
    private lateinit var adapter: SelectUsersAdapter
    private lateinit var search: SearchDebouncer
    private var request: UsersRequest? = null
    private var isLoading = false
    private var hasMore = true
    private var creating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbar.toolbar.setTitle(R.string.new_group_call)
        binding.toolbar.toolbar.setNavigationOnClickListener { finish() }
        binding.search.editSearch.setHint(R.string.search_users)

        adapter = SelectUsersAdapter(MAX_MEMBERS, onLimitReached = {
            Toast.makeText(this, getString(R.string.members_limit, MAX_MEMBERS), Toast.LENGTH_SHORT).show()
        }, onSelectionChanged = { updateSelection() })
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewUsers.layoutManager = layoutManager
        binding.recyclerViewUsers.adapter = adapter
        binding.recyclerViewUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 5) loadMore()
            }
        })

        binding.btnStartVoice.setOnClickListener { create(isVideo = false) }
        binding.btnStartVideo.setOnClickListener { create(isVideo = true) }

        search = SearchDebouncer(binding.search) { reload() }
        updateSelection()
        reload()
    }

    private fun reload() {
        request = UsersRequest.UsersRequestBuilder()
            .setLimit(PAGE_SIZE)
            .hideBlockedUsers(true)
            .apply { if (search.query.isNotEmpty()) setSearchKeyword(search.query) }
            .build()
        hasMore = true
        isLoading = false
        loadMore(replacing = true)
    }

    private fun loadMore(replacing: Boolean = false) {
        val request = request ?: return
        if (isLoading || !hasMore) return
        isLoading = true
        val generation = search.generation
        request.fetchNext(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                if (isDestroyed || generation != search.generation) return
                isLoading = false
                hasMore = users.size >= PAGE_SIZE
                val me = CometChat.getLoggedInUser()?.uid
                val page = users.filter { it.uid != me }
                if (replacing) adapter.submit(page) else adapter.append(page)
            }

            override fun onError(e: CometChatException) {
                if (isDestroyed || generation != search.generation) return
                isLoading = false
                hasMore = false
            }
        })
    }

    private fun updateSelection() {
        val count = adapter.selected.size
        binding.tvSelected.text = getString(R.string.members_selected, count)
        val enabled = count > 0 && !creating
        binding.btnStartVoice.isEnabled = enabled
        binding.btnStartVideo.isEnabled = enabled
    }

    /** "Ana, Ben, Cai +2" when no name was typed. */
    private fun groupName(members: List<User>): String {
        val typed = binding.etGroupName.text?.toString()?.trim().orEmpty()
        if (typed.isNotEmpty()) return typed
        val names = members.map { it.name ?: it.uid }
        val shown = names.take(3).joinToString(", ")
        return if (names.size > 3) "$shown +${names.size - 3}" else shown
    }

    /**
     * Creates the group, adds the members, then rings it. The members are added in a second step
     * so a member who cannot be added does not stop the group being created.
     */
    private fun create(isVideo: Boolean) {
        if (creating || !CallLauncher.ensurePermissions(this, isVideo)) return
        val members = adapter.selected.values.toList()
        if (members.isEmpty()) return
        setCreating(true)

        val group = Group("group-" + UUID.randomUUID(), groupName(members), CometChatConstants.GROUP_TYPE_PRIVATE, null)
        CometChat.createGroup(group, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(created: Group) {
                val groupMembers = members.map { GroupMember(it.uid, CometChatConstants.SCOPE_PARTICIPANT) }
                CometChat.addMembersToGroup(created.guid, groupMembers, emptyList(), object : CometChat.CallbackListener<HashMap<String, String>>() {
                    override fun onSuccess(result: HashMap<String, String>) {
                        val added = result.values.count { it.equals("success", ignoreCase = true) }
                        Log.i(TAG, "created ${created.guid}; added $added of ${members.size}")
                        if (added < members.size) {
                            Toast.makeText(this@NewGroupCallActivity, getString(R.string.members_added_partial, added, members.size), Toast.LENGTH_LONG).show()
                        }
                        ring(created, isVideo)
                    }

                    override fun onError(e: CometChatException) {
                        failed(e)
                    }
                })
            }

            override fun onError(e: CometChatException) {
                failed(e)
            }
        })
    }

    private fun ring(group: Group, isVideo: Boolean) {
        setResult(RESULT_OK)
        CallLauncher.start(this, CallTarget(group.guid, group.name ?: group.guid, group.icon, isGroup = true), isVideo)
        finish()
    }

    private fun failed(e: CometChatException) {
        Log.e(TAG, "creating the group failed: ${e.code} ${e.message}")
        if (isDestroyed) return
        setCreating(false)
        Toast.makeText(this, getString(R.string.create_group_failed, e.message), Toast.LENGTH_LONG).show()
    }

    private fun setCreating(value: Boolean) {
        creating = value
        binding.progress.visibility = if (value) View.VISIBLE else View.GONE
        updateSelection()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::search.isInitialized) search.cancel()
    }
}
