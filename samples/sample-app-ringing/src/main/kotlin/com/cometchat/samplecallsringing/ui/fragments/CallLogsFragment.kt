package com.cometchat.samplecallsringing.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.databinding.FragmentCallLogsBinding
import com.cometchat.samplecallsringing.ui.adapters.CallLogsAdapter

class CallLogsFragment : Fragment() {

    private var _binding: FragmentCallLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CallLogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        fetchCallLogs()
    }

    override fun onResume() {
        super.onResume()
        fetchCallLogs()
    }

    private fun setupRecyclerView() {
        adapter = CallLogsAdapter(mutableListOf())
        binding.recyclerViewCallLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCallLogs.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { fetchCallLogs() }
    }

    private fun fetchCallLogs() {
        Repository.fetchCallLogs(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(messages: List<BaseMessage>) {
                if (!isAdded) return
                adapter.updateList(messages)
                binding.swipeRefresh.isRefreshing = false
                binding.tvEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onError(e: CometChatException) {
                if (!isAdded) return
                binding.swipeRefresh.isRefreshing = false
                binding.tvEmpty.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
