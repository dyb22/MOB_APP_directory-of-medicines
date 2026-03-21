package com.example.mobile.presentation.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.mobile.R
import com.example.mobile.di.AppContainer
import com.example.mobile.presentation.MainActivity
import kotlinx.coroutines.launch

/**
 * Экран содержимого папки закладок: список препаратов.
 * Открывается поверх BookmarksFragment из MainActivity.openBookmarkDetail.
 * При клике по препарату — DrugDetailFragment поверх этого фрагмента.
 */
class BookmarkDetailFragment : Fragment() {

    companion object {
        private const val ARG_BOOKMARK = "arg_bookmark"

        fun newInstance(bookmark: Bookmark): BookmarkDetailFragment {
            return BookmarkDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKMARK + "_id", bookmark.id)
                    putString(ARG_BOOKMARK + "_name", bookmark.name)
                }
            }
        }
    }

    private var bookmarkId: String = ""
    private var bookmarkName: String = ""
    private lateinit var drugsList: ListView
    private lateinit var adapter: ArrayAdapter<Drug>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookmarkId = it.getString(ARG_BOOKMARK + "_id", "")
            bookmarkName = it.getString(ARG_BOOKMARK + "_name", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_bookmark_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageButton>(R.id.button_back)
        val titleText = view.findViewById<TextView>(R.id.text_title)
        drugsList = view.findViewById(R.id.drugs_list)

        titleText.text = bookmarkName
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack() // назад к списку папок
        }

        val drugs = mutableListOf<Drug>()
        adapter = object : ArrayAdapter<Drug>(requireContext(), R.layout.item_bookmark_drug, drugs) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark_drug, parent, false)
                val item = getItem(position) ?: return view
                view.findViewById<TextView>(R.id.text_manufacturer).text = item.manufacturer.ifBlank { "" }
                view.findViewById<TextView>(R.id.text_name).text = item.name
                view.findViewById<TextView>(R.id.text_price).text = item.price
                return view
            }
        }
        drugsList.adapter = adapter

        drugsList.setOnItemClickListener { _, _, position, _ ->
            val drug = adapter.getItem(position) ?: return@setOnItemClickListener
            (activity as? MainActivity)?.openDrugDetail(drug, this)
        }

        loadDrugs()
    }

    override fun onResume() {
        // обновить список при возврате на экран
        super.onResume()
        loadDrugs()
    }

    private fun loadDrugs() {
        lifecycleScope.launch {
            // Firestore или локальный гость
            val list = AppContainer.drugRepository.getDrugsInBookmark(bookmarkId)
            adapter.clear()
            adapter.addAll(list)
            adapter.notifyDataSetChanged()
        }
    }
}

