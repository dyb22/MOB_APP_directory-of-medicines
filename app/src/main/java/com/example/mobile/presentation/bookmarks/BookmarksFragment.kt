package com.example.mobile.presentation.bookmarks

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.domain.model.Bookmark
import com.example.mobile.R
import com.example.mobile.di.AppContainer
import com.example.mobile.presentation.MainActivity
import kotlinx.coroutines.launch

/**
 * Экран списка папок закладок. Toolbar с меню создания папки.
 * Клик по папке: если есть pendingDrug — добавить и вернуться на поиск; иначе — открыть содержимое.
 * Долгое нажатие: удаление или переименование папки через диалоги.
 */
class BookmarksFragment : Fragment() {

    private val bookmarks = mutableListOf<Bookmark>()
    private lateinit var adapter: ArrayAdapter<Bookmark>
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // меню с кнопкой создания папки
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
            if (pendingDrug != null) { // добавить в выбранную папку
                lifecycleScope.launch {
                    AppContainer.drugRepository.addDrugToBookmark(bookmark.id, pendingDrug)
                    mainActivity.getAndClearPendingDrugForBookmark()
                    mainActivity.switchToSearch()
                }
            } else {
                mainActivity.openBookmarkDetail(bookmark)
            }
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val bookmark = adapter.getItem(position) ?: return@setOnItemLongClickListener false

            val options = arrayOf(
                getString(R.string.bookmarks_menu_delete),
                getString(R.string.bookmarks_menu_rename)
            )

            AlertDialog.Builder(requireContext())
                .setTitle(bookmark.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // удаление
                            AlertDialog.Builder(requireContext())
                                .setMessage(R.string.bookmarks_dialog_delete_message)
                                .setPositiveButton(R.string.bookmarks_dialog_delete_confirm) { _, _ ->
                                    lifecycleScope.launch {
                                        AppContainer.drugRepository.removeBookmark(bookmark.id)
                                        loadBookmarks()
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }

                        else -> { // переименование
                            val editText = EditText(requireContext()).apply {
                                setText(bookmark.name)
                                setSelection(bookmark.name.length)
                                hint = getString(R.string.bookmarks_dialog_rename_hint)
                            }

                            AlertDialog.Builder(requireContext())
                                .setTitle(R.string.bookmarks_dialog_rename_title)
                                .setView(editText)
                                .setPositiveButton(R.string.bookmarks_dialog_save) { _, _ ->
                                    val newName = editText.text.toString().trim()
                                    if (newName.isNotEmpty() && newName != bookmark.name) {
                                        lifecycleScope.launch {
                                            AppContainer.drugRepository.renameBookmark(bookmark.id, newName)
                                            loadBookmarks()
                                        }
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                    }
                }
                .show()

            true
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

    // Вызов из MainActivity при открытии вкладки
    fun refreshBookmarks() {
        if (isAdded) loadBookmarks()
    }

    /** Загрузка папок из репозитория (Firestore или гость) */
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

    /** Диалог ввода имени новой папки закладок */
    private fun showCreateBookmarkDialog() {
        val editText = EditText(requireContext()).apply {
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

