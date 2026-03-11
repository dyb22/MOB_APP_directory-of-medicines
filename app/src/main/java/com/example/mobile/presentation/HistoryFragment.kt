package com.example.mobile.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobile.R

class HistoryFragment : Fragment() {

    // Пока просто заглушка — реальные данные истории можно будет передавать позже
    private val historyItems = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var emptyText: TextView
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        emptyText = view.findViewById(R.id.empty_text)
        listView = view.findViewById(R.id.history_list)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, historyItems)
        listView.adapter = adapter
        updateEmptyState()
        return view
    }

    private fun updateEmptyState() {
        if (historyItems.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }
}

