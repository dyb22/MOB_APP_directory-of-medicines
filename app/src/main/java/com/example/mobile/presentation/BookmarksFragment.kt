package com.example.mobile.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobile.R

class BookmarksFragment : Fragment() {

    private val bookmarks = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var emptyText: TextView
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks, container, false)
        emptyText = view.findViewById(R.id.empty_text)
        listView = view.findViewById(R.id.bookmarks_list)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, bookmarks)
        listView.adapter = adapter
        updateEmptyState()
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bookmarks, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_bookmark -> {
                showCreateBookmarkDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCreateBookmarkDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.bookmarks_dialog_hint)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.bookmarks_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.bookmarks_dialog_create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    bookmarks.add(name)
                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateEmptyState() {
        if (bookmarks.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }
}

