package com.example.domain.model

import java.io.Serializable

/** Модель препарата. Serializable для передачи между фрагментами через arguments. */
data class Drug(
    val id: String,
    val name: String,
    val manufacturer: String,
    val price: String,
    val description: String
) : Serializable

