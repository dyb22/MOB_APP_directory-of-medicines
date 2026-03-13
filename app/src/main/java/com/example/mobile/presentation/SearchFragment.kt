package com.example.mobile.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mobile.di.AppContainer
import com.example.domain.usecase.drug.SearchDrugsUseCase
import com.example.mobile.R
import com.example.mobile.presentation.search.SearchResultsAdapter
import com.example.mobile.presentation.search.SearchUiState
import com.example.mobile.presentation.search.SearchViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels {
        val useCase = SearchDrugsUseCase(AppContainer.drugRepository)
        SearchViewModelFactory(useCase)
    }

    private lateinit var searchInput: EditText
    private lateinit var cameraButton: ImageButton
    private lateinit var resultsTitle: TextView
    private lateinit var nothingFoundText: TextView
    private lateinit var resultsList: ListView
    private lateinit var resultsAdapter: SearchResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput = view.findViewById(R.id.search_input)
        cameraButton = view.findViewById(R.id.button_camera)
        resultsTitle = view.findViewById(R.id.text_results_title)
        nothingFoundText = view.findViewById(R.id.text_nothing_found)
        resultsList = view.findViewById(R.id.search_results_list)

        resultsAdapter = SearchResultsAdapter(
            requireContext(),
            mutableListOf()
        )
        resultsAdapter.onAddToBookmark = { drug ->
            (activity as? MainActivity)?.requestAddDrugToBookmark(drug)
        }
        resultsList.adapter = resultsAdapter

        resultsList.setOnItemClickListener { _, _, position, _ ->
            val item = resultsAdapter.getItem(position) ?: return@setOnItemClickListener
            (activity as? MainActivity)?.openDrugDetail(item)
        }

        // Пока кнопка с иконкой камеры просто запускает поиск по введённому тексту
        cameraButton.setOnClickListener {
            triggerSearch()
        }

        // Запуск поиска по нажатию Enter (действие "Поиск" на клавиатуре)
        searchInput.setOnEditorActionListener { _, _, _ ->
            triggerSearch()
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                renderState(state)
            }
        }
    }

    private fun triggerSearch() {
        viewModel.onQueryChanged(searchInput.text.toString())
        viewModel.onSearchClicked()
    }

    private fun renderState(state: SearchUiState) {
        val hasResults = state.results.isNotEmpty()
        resultsTitle.visibility = if (state.hasSearched && hasResults) View.VISIBLE else View.GONE
        nothingFoundText.visibility = if (state.hasSearched && !hasResults) View.VISIBLE else View.GONE
        resultsList.visibility = if (state.hasSearched && hasResults) View.VISIBLE else View.GONE
        resultsAdapter.clear()
        resultsAdapter.addAll(state.results)
        resultsAdapter.notifyDataSetChanged()
    }
}
