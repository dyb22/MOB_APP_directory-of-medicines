package com.example.domain.model

// Препарат и время просмотра в миллисекундах
data class SearchHistoryItem(
    val drug: Drug,
    val viewedAtMillis: Long
)

