package com.cometchat.samplecallsringing.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.core.CallLogRequest
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.model.CallLog
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.databinding.FragmentCallLogsBinding
import com.cometchat.samplecallsringing.ui.activity.CallDetailsActivity
import com.cometchat.samplecallsringing.ui.adapters.CallLogsAdapter

/**
 * Your call history from the Calls SDK. Reloads every time the tab is shown, so a call that just
 * ended is listed; pull to refresh, and scroll for older calls.
 */
class CallLogsFragment : Fragment() {

    private var _binding: FragmentCallLogsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CallLogsAdapter
    private var request: CallLogRequest? = null
    private var isLoading = false
    private var hasMore = true
    /** Bumped on every reload, so a page from an older reload is dropped. */
    private var generation = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCallLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CallLogsAdapter { log ->
            CallDetailsActivity.start(requireContext(), log)
        }
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCallLogs.layoutManager = layoutManager
        binding.recyclerViewCallLogs.adapter = adapter
        binding.recyclerViewCallLogs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 3) loadMore()
            }
        })
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.btnRetry.setOnClickListener { reload() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        generation++
        request = CallLogRequest.CallLogRequestBuilder().setLimit(PAGE_SIZE).build()
        hasMore = true
        isLoading = false
        loadMore(replacing = true)
    }

    private fun loadMore(replacing: Boolean = false) {
        val request = request ?: return
        if (isLoading || !hasMore) return
        isLoading = true
        val thisGeneration = generation
        if (replacing && adapter.itemCount == 0) {
            binding.layoutLoading.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }

        request.fetchNext(object : CometChatCalls.CallbackListener<List<CallLog>>() {
            override fun onSuccess(logs: List<CallLog>) {
                if (_binding == null || thisGeneration != generation) return
                isLoading = false
                hasMore = logs.size >= PAGE_SIZE
                if (replacing) adapter.submit(logs) else adapter.append(logs)
                showState(error = null)
            }

            override fun onError(e: CallsException) {
                if (_binding == null || thisGeneration != generation) return
                isLoading = false
                hasMore = false
                // A failed refresh keeps the rows already on screen.
                showState(error = if (adapter.itemCount == 0) e.message.orEmpty() else null)
            }
        })
    }

    private fun showState(error: String?) {
        binding.swipeRefresh.isRefreshing = false
        binding.layoutLoading.visibility = View.GONE
        val empty = adapter.itemCount == 0
        binding.layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.tvEmptyTitle.setText(R.string.no_call_logs)
        binding.tvEmptyMessage.text = if (error != null) getString(R.string.history_error, error) else getString(R.string.no_call_logs_message)
        binding.btnRetry.visibility = if (error != null) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
