package com.example.mobile.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.domain.model.Drug
import com.example.mobile.R
import com.example.mobile.di.AppContainer
import com.example.mobile.presentation.MainActivity
import com.example.mobile.presentation.search.SearchResultsAdapter
import kotlinx.coroutines.launch

/**
 * Экран истории просмотров. Список препаратов, открытых с экрана поиска.
 * Пустой текст при отсутствии записей. Клик — карточка, кнопка — добавить в закладки.
 */
class HistoryFragment : Fragment() {

    private val historyDrugs = mutableListOf<Drug>()
    private lateinit var adapter: SearchResultsAdapter
    private lateinit var emptyText: TextView
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(view.findViewById(R.id.toolbar))
        emptyText = view.findViewById(R.id.empty_text)
        listView = view.findViewById(R.id.history_list)
        adapter = SearchResultsAdapter(requireContext(), mutableListOf())
        adapter.onItemClick = { drug ->
            (activity as? MainActivity)?.openDrugDetail(drug)
        }
        adapter.onAddToBookmark = { drug ->
            (activity as? MainActivity)?.requestAddDrugToBookmark(drug)
        }
        listView.adapter = adapter
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory() // данные после возврата на экран
    }

    override fun onDestroyView() {
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        super.onDestroyView()
    }

    /** Показать «пусто» или список в зависимости от наличия данных */
    private fun updateEmptyState() {
        if (historyDrugs.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }

    /** Загрузка истории из репозитория, обновление адаптера */
    private fun loadHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = AppContainer.drugRepository.getSearchHistory()
            historyDrugs.clear()
            historyDrugs.addAll(items.map { it.drug })
            adapter.clear()
            adapter.addAll(historyDrugs)
            adapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    // Вызов из MainActivity
    fun refreshHistory() {
        if (isAdded) {
            loadHistory()
        }
    }
}

