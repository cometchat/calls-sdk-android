package com.cometchat.samplecallsringing.ui.fragments

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.cometchat.samplecallsringing.databinding.ViewSearchFieldBinding

/**
 * Wires a search field: [onSearch] runs 300 ms after the last keystroke, and the clear button
 * empties the field. Each search gets a new [generation], so a list can drop a response that
 * arrives after a newer search started.
 */
class SearchDebouncer(
    private val searchField: ViewSearchFieldBinding,
    private val onSearch: (query: String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    var generation = 0
        private set

    init {
        searchField.btnClear.setOnClickListener { searchField.editSearch.setText("") }
        searchField.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                searchField.btnClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                pending?.let { handler.removeCallbacks(it) }
                pending = Runnable {
                    generation++
                    onSearch(query)
                }.also { handler.postDelayed(it, DEBOUNCE_MS) }
            }
        })
    }

    val query: String get() = searchField.editSearch.text?.toString()?.trim().orEmpty()

    fun cancel() {
        pending?.let { handler.removeCallbacks(it) }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
