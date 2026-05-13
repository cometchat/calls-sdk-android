package com.cometchat.samplecallsvoip.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.User
import com.cometchat.samplecallsvoip.data.repository.Repository
import com.cometchat.samplecallsvoip.databinding.FragmentUsersBinding
import com.cometchat.samplecallsvoip.ui.activity.OutgoingCallActivity
import com.cometchat.samplecallsvoip.ui.adapters.UsersAdapter
import com.cometchat.samplecallsvoip.utils.AppUtils

class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UsersAdapter
    private var isLoading = false
    private var hasMore = true
    private var isSearching = false
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        fetchUsers()
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                searchRunnable = Runnable {
                    if (query.isEmpty()) {
                        isSearching = false
                        adapter.updateList(emptyList())
                        Repository.resetUsersRequest()
                        hasMore = true
                        fetchUsers()
                    } else {
                        isSearching = true
                        searchUsers(query)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun searchUsers(keyword: String) {
        Repository.searchUsers(keyword, object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                if (!isAdded) return
                val loggedInUid = AppUtils.getLoggedInUid(requireContext())
                val filtered = users.filter { it.uid != loggedInUid }
                adapter.updateList(filtered)
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onError(e: CometChatException) {
                if (!isAdded) return
                adapter.updateList(emptyList())
                binding.tvEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = UsersAdapter(
            mutableListOf(),
            onVoiceCallClick = { user ->
                initiateCall(user, CometChatConstants.CALL_TYPE_AUDIO)
            },
            onVideoCallClick = { user ->
                initiateCall(user, CometChatConstants.CALL_TYPE_VIDEO)
            }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUsers.layoutManager = layoutManager
        binding.recyclerViewUsers.adapter = adapter

        binding.recyclerViewUsers.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (!isLoading && hasMore && !isSearching && lastVisibleItem >= totalItemCount - 5) {
                    fetchUsers()
                }
            }
        })
    }

    private fun fetchUsers() {
        isLoading = true
        Repository.fetchUsers(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                if (!isAdded) return
                isLoading = false
                if (users.isEmpty()) {
                    hasMore = false
                    return
                }
                val loggedInUid = AppUtils.getLoggedInUid(requireContext())
                val filtered = users.filter { it.uid != loggedInUid }
                adapter.appendList(filtered)
                binding.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }

            override fun onError(e: CometChatException) {
                if (!isAdded) return
                isLoading = false
                if (adapter.itemCount == 0) {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun initiateCall(user: User, callType: String) {
        val call = Call(user.uid, CometChatConstants.RECEIVER_TYPE_USER, callType)
        Repository.initiateCall(call, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(outgoingCall: Call) {
                if (!isAdded) return
                val intent = Intent(requireContext(), OutgoingCallActivity::class.java).apply {
                    putExtra("SESSION_ID", outgoingCall.sessionId)
                    putExtra("RECEIVER_NAME", user.name)
                    putExtra("RECEIVER_AVATAR", user.avatar)
                    putExtra("CALL_TYPE", callType)
                }
                startActivity(intent)
            }

            override fun onError(e: CometChatException) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Call failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
