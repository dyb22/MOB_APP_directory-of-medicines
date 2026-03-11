package com.example.data.repository

import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.domain.model.SearchHistoryItem
import com.example.domain.repository.DrugRepository

/**
 * Сейчас это полностью мок-реализация без сетевых вызовов.
 * Позже сюда можно будет вернуть обращение к реальному API.
 */
class DrugRepositoryImpl : DrugRepository {

    // Простейший моковый список лекарств
    private val mockDrugs = listOf(
        Drug(
            id = "1",
            name = "Ibuprofen",
            manufacturer = "Mock Pharma",
            price = "200 рублей",
            description = "Классический нестероидный противовоспалительный препарат. Снижает боль, воспаление и температуру."
        ),
        Drug(
            id = "2",
            name = "IbuHEXAL",
            manufacturer = "HEXAL AG",
            price = "210 рублей",
            description = "Препарат на основе ибупрофена европейского производства. Используется при боли и воспалении."
        ),
        Drug(
            id = "3",
            name = "IbuProLong",
            manufacturer = "Health Corp",
            price = "230 рублей",
            description = "Форма ибупрофена пролонгированного действия для более длительного эффекта."
        ),
        Drug(
            id = "4",
            name = "Paracetamol",
            manufacturer = "Acme Labs",
            price = "90 рублей",
            description = "Жаропонижающий и обезболивающий препарат, часто используемый при простуде и головной боли."
        ),
        Drug(
            id = "5",
            name = "Aspirin",
            manufacturer = "Bayer",
            price = "120 рублей",
            description = "Классический препарат на основе ацетилсалициловой кислоты. Часто применяется для разжижения крови."
        ),
        Drug(
            id = "6",
            name = "Ibuklin",
            manufacturer = "Dr. Reddy's",
            price = "250 рублей",
            description = "Комбинированный препарат (ибупрофен + парацетамол) для более выраженного обезболивающего эффекта."
        )
    )

    override suspend fun searchDrugs(query: String): List<Drug> {
        if (query.isBlank()) return emptyList()
        val normalized = query.trim()
        return mockDrugs.filter { drug ->
            drug.name.startsWith(normalized, ignoreCase = true)
        }
    }

    override suspend fun getDrugById(id: String): Drug? =
        mockDrugs.find { it.id == id }

    override suspend fun getBookmarks(): List<Bookmark> = emptyList()

    override suspend fun addBookmark(bookmark: Bookmark) {
        // TODO: implement persistence (моки не сохраняют)
    }

    override suspend fun removeBookmark(id: String) {
        // TODO: implement persistence (моки не сохраняют)
    }

    override suspend fun getSearchHistory(): List<SearchHistoryItem> = emptyList()

    override suspend fun clearSearchHistory() {
        // TODO: implement persistence (моки не сохраняют)
    }
}

