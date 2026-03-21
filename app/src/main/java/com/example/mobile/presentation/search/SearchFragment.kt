package com.example.mobile.presentation.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.domain.usecase.drug.SearchDrugsUseCase
import com.example.mobile.R
import com.example.mobile.di.AppContainer
import com.example.mobile.presentation.MainActivity
import com.example.mobile.presentation.SearchViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Экран поиска препаратов. Поле ввода, кнопка камеры, список результатов.
 * Подписывается на SearchViewModel.uiState. При клике — запись в историю и открытие карточки.
 * Поддерживает состояние «нет сети» и оверлей загрузки.
 */
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
    private lateinit var loadingOverlay: View
    private lateinit var noNetworkContainer: View
    private lateinit var resultsAdapter: SearchResultsAdapter
    /** Для повторного рендера в onResume (например после возврата с карточки) */
    private var lastUiState: SearchUiState? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view as FrameLayout
        searchInput = view.findViewById(R.id.search_input)
        cameraButton = view.findViewById(R.id.button_camera)
        resultsTitle = view.findViewById(R.id.text_results_title)
        nothingFoundText = view.findViewById(R.id.text_nothing_found)
        resultsList = view.findViewById(R.id.search_results_list)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        noNetworkContainer = view.findViewById(R.id.no_network_container)

        // Оверлей на весь контейнер
        loadingOverlay.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        loadingOverlay.bringToFront()

        resultsAdapter = SearchResultsAdapter(
            requireContext(),
            mutableListOf()
        )
        resultsAdapter.onAddToBookmark = { drug ->
            (activity as? MainActivity)?.requestAddDrugToBookmark(drug)
        }
        resultsAdapter.onItemClick = { drug ->
            // Запись в историю и открытие карточки
            viewLifecycleOwner.lifecycleScope.launch {
                AppContainer.drugRepository.addToSearchHistory(drug)
            }
            (activity as? MainActivity)?.openDrugDetail(drug)
        }
        resultsList.adapter = resultsAdapter

        // Системная камера
        cameraButton.setOnClickListener {
            openCamera()
        }

        // Поиск по действию на клавиатуре
        searchInput.setOnEditorActionListener { _, _, _ ->
            triggerSearch()
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Подписка на состояние поиска
            viewModel.uiState.collectLatest { state ->
                lastUiState = state
                renderState(state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lastUiState?.let { renderState(it) } // восстановить UI после возврата
    }

    /** Читает текст из поля и запускает поиск через ViewModel */
    private fun triggerSearch() {
        viewModel.onQueryChanged(searchInput.text.toString())
        viewModel.onSearchClicked()
    }

    /** Запуск системного приложения камеры (ACTION_IMAGE_CAPTURE) */
    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = requireContext().packageManager
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    /** Отображает состояние: загрузка, результаты, пусто, нет сети */
    private fun renderState(state: SearchUiState) {
        if (state.isNoNetwork) {
            showNoNetworkState()
            return
        }

        noNetworkContainer.visibility = View.GONE

        if (state.isLoading) {
            loadingOverlay.visibility = View.VISIBLE
            loadingOverlay.bringToFront()
            resultsTitle.visibility = View.GONE
            nothingFoundText.visibility = View.GONE
            resultsList.visibility = View.GONE
            return
        }

        loadingOverlay.visibility = View.GONE

        val hasResults = state.results.isNotEmpty()
        resultsTitle.visibility = if (state.hasSearched && hasResults) View.VISIBLE else View.GONE
        nothingFoundText.visibility = if (state.hasSearched && !hasResults) View.VISIBLE else View.GONE
        resultsList.visibility = if (state.hasSearched && hasResults) View.VISIBLE else View.GONE
        resultsAdapter.clear()
        resultsAdapter.addAll(state.results)
        resultsAdapter.notifyDataSetChanged()
    }

    private fun showNoNetworkState() {
        // Только блок «нет сети»
        noNetworkContainer.visibility = View.VISIBLE
        loadingOverlay.visibility = View.GONE
        resultsTitle.visibility = View.GONE
        nothingFoundText.visibility = View.GONE
        resultsList.visibility = View.GONE
    }
}

