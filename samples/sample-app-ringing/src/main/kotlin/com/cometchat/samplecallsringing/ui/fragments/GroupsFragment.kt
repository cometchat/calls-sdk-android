package com.cometchat.samplecallsringing.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.calls.CallLauncher
import com.cometchat.samplecallsringing.calls.CallTarget
import com.cometchat.samplecallsringing.databinding.FragmentGroupsBinding
import com.cometchat.samplecallsringing.ui.activity.NewGroupCallActivity
import com.cometchat.samplecallsringing.ui.adapters.GroupsAdapter

/**
 * The groups you have joined. The call buttons ring every member: whoever answers joins the same
 * session, and the call carries on when one of them leaves. **+** creates a group to ring.
 */
class GroupsFragment : Fragment() {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GroupsAdapter
    private lateinit var search: SearchDebouncer
    private var request: GroupsRequest? = null
    private var isLoading = false
    private var hasMore = true

    private val newGroup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && _binding != null) reload()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.search.editSearch.setHint(R.string.search_groups)
        adapter = GroupsAdapter(
            onVoiceCallClick = { call(it, isVideo = false) },
            onVideoCallClick = { call(it, isVideo = true) }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGroups.layoutManager = layoutManager
        binding.recyclerViewGroups.adapter = adapter
        binding.recyclerViewGroups.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 5) loadMore()
            }
        })
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener { reload() }

        val openNewGroup = View.OnClickListener {
            newGroup.launch(Intent(requireContext(), NewGroupCallActivity::class.java))
        }
        binding.btnNewGroup.setOnClickListener(openNewGroup)
        binding.btnEmptyNewGroup.setOnClickListener(openNewGroup)

        search = SearchDebouncer(binding.search) { reload() }
        reload()
    }

    private fun reload() {
        request = GroupsRequest.GroupsRequestBuilder()
            .setLimit(PAGE_SIZE)
            .joinedOnly(true)
            .apply { if (search.query.isNotEmpty()) setSearchKeyWord(search.query) }
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
        if (replacing && adapter.itemCount == 0) binding.swipeRefresh.isRefreshing = true

        request.fetchNext(object : CometChat.CallbackListener<List<Group>>() {
            override fun onSuccess(groups: List<Group>) {
                if (_binding == null || generation != search.generation) return
                isLoading = false
                hasMore = groups.size >= PAGE_SIZE
                if (replacing) adapter.submit(groups) else adapter.append(groups)
                showState()
            }

            override fun onError(e: CometChatException) {
                if (_binding == null || generation != search.generation) return
                isLoading = false
                hasMore = false
                showState()
            }
        })
    }

    private fun showState() {
        binding.swipeRefresh.isRefreshing = false
        val empty = adapter.itemCount == 0
        binding.layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        val searching = search.query.isNotEmpty()
        binding.tvEmptyTitle.text = if (searching) getString(R.string.no_groups_matching, search.query) else getString(R.string.no_groups_title)
        binding.tvEmptyMessage.visibility = if (searching) View.GONE else View.VISIBLE
        binding.btnEmptyNewGroup.visibility = if (searching) View.GONE else View.VISIBLE
    }

    private fun call(group: Group, isVideo: Boolean) {
        CallLauncher.start(requireActivity(), CallTarget(group.guid, group.name ?: group.guid, group.icon, isGroup = true), isVideo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        search.cancel()
        _binding = null
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
