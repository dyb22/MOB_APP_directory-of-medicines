package com.example.domain.model

import java.time.Instant

data class SearchHistoryItem(
    val id: String,
    val query: String,
    val viewedAt: Instant
)

