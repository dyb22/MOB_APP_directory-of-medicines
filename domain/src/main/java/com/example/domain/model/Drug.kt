package com.example.domain.model

import java.io.Serializable

data class Drug(
    val id: String,
    val name: String,
    val manufacturer: String,
    val price: String,
    val description: String
) : Serializable

