package com.example.mobile.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.domain.model.Bookmark
import com.example.mobile.R
import com.example.mobile.di.AppContainer
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment() {

    private val bookmarks = mutableListOf<Bookmark>()
    private lateinit var adapter: ArrayAdapter<Bookmark>
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
        adapter = object : ArrayAdapter<Bookmark>(requireContext(), R.layout.item_bookmark, android.R.id.text1, bookmarks) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                v.findViewById<android.widget.TextView>(android.R.id.text1).text = getItem(position)?.name ?: ""
                return v
            }
        }
        listView.adapter = adapter

        loadBookmarks()

        listView.setOnItemClickListener { _, _, position, _ ->
            val bookmark = adapter.getItem(position) ?: return@setOnItemClickListener
            val mainActivity = activity as? MainActivity ?: return@setOnItemClickListener
            val pendingDrug = mainActivity.getPendingDrugForBookmark()
            if (pendingDrug != null) {
                lifecycleScope.launch {
                    AppContainer.drugRepository.addDrugToBookmark(bookmark.id, pendingDrug)
                    mainActivity.getAndClearPendingDrugForBookmark()
                    mainActivity.switchToSearch()
                }
            } else {
                mainActivity.openBookmarkDetail(bookmark)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

    /** Обновить список закладок при переходе на вкладку (в т.ч. после выхода из профиля). */
    fun refreshBookmarks() {
        if (isAdded) loadBookmarks()
    }

    private fun loadBookmarks() {
        lifecycleScope.launch {
            val list = AppContainer.drugRepository.getBookmarks()
            bookmarks.clear()
            bookmarks.addAll(list)
            adapter.notifyDataSetChanged()
        }
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
                    lifecycleScope.launch {
                        AppContainer.drugRepository.addBookmark(Bookmark(id = "", name = name))
                        loadBookmarks()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
