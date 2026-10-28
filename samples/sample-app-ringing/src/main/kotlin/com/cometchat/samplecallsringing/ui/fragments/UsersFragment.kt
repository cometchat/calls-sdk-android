package com.cometchat.samplecallsringing.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.calls.CallLauncher
import com.cometchat.samplecallsringing.calls.CallTarget
import com.cometchat.samplecallsringing.databinding.FragmentUsersBinding
import com.cometchat.samplecallsringing.ui.adapters.UsersAdapter

/** Everyone in the app, with presence. The phone and video buttons ring that person. */
class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UsersAdapter
    private lateinit var search: SearchDebouncer
    private var request: UsersRequest? = null
    private var isLoading = false
    private var hasMore = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.search.editSearch.setHint(R.string.search_users)
        adapter = UsersAdapter(
            onVoiceCallClick = { call(it, isVideo = false) },
            onVideoCallClick = { call(it, isVideo = true) }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUsers.layoutManager = layoutManager
        binding.recyclerViewUsers.adapter = adapter
        binding.recyclerViewUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 5) loadMore()
            }
        })

        search = SearchDebouncer(binding.search) { reload() }
        reload()
    }

    /** Starts the list again for the current search. */
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
        if (replacing && adapter.itemCount == 0) binding.progress.visibility = View.VISIBLE

        request.fetchNext(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                // A newer search started while this page was loading.
                if (_binding == null || generation != search.generation) return
                isLoading = false
                hasMore = users.size >= PAGE_SIZE
                val me = CometChat.getLoggedInUser()?.uid
                val page = users.filter { it.uid != me }
                if (replacing) adapter.submit(page) else adapter.append(page)
                showState()
            }

            override fun onError(e: CometChatException) {
                if (_binding == null || generation != search.generation) return
                isLoading = false
                hasMore = false
                if (replacing) adapter.submit(emptyList())
                showState()
            }
        })
    }

    private fun showState() {
        binding.progress.visibility = View.GONE
        val empty = adapter.itemCount == 0
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.tvEmpty.text = if (search.query.isEmpty()) getString(R.string.no_users_found)
        else getString(R.string.no_users_matching, search.query)
    }

    private fun call(user: User, isVideo: Boolean) {
        CallLauncher.start(requireActivity(), CallTarget(user.uid, user.name ?: user.uid, user.avatar, isGroup = false), isVideo)
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
