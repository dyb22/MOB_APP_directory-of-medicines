package com.example.domain.model

/**
 * Элемент истории просмотров: конкретный препарат и момент просмотра.
 * Используем обычный timestamp в миллисекундах, чтобы избежать проблем с java.time на старых устройствах.
 */
data class SearchHistoryItem(
    val drug: Drug,
    val viewedAtMillis: Long
)

