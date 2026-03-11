package com.example.domain.usecase.drug

import com.example.domain.model.Drug
import com.example.domain.repository.DrugRepository

class SearchDrugsUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke(query: String): List<Drug> {
        return repository.searchDrugs(query)
    }
}

