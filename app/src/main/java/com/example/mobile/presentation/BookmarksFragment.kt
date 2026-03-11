package com.example.mobile.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mobile.R

class BookmarksFragment : Fragment() {

    private val bookmarks = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
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
        return inflater.inflate(R.layout.fragment_bookmarks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        ContextCompat.getDrawable(requireContext(), R.drawable.ic_menuicon)?.let { toolbar.setOverflowIcon(it) }
        listView = view.findViewById(R.id.bookmarks_list)
        adapter = ArrayAdapter(requireContext(), R.layout.item_bookmark, android.R.id.text1, bookmarks)
        listView.adapter = adapter
    }

    override fun onDestroyView() {
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        super.onDestroyView()
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
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

