package com.example.domain.usecase.drug

import com.example.domain.model.Drug
import com.example.domain.repository.DrugRepository

class GetDrugDetailsUseCase(
    private val repository: DrugRepository
) {
    suspend operator fun invoke(id: String): Drug? {
        return repository.getDrugById(id)
    }
}

