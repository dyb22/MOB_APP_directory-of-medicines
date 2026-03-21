package com.example.data.mapper

import com.example.data.storage.local.entity.DrugEntity
import com.example.data.storage.remote.dto.DrugResultDto
import com.example.domain.model.Drug

private const val PRICE_ZERO = "0 рублей"

/**
 * Преобразование DTO OpenFDA в доменную модель Drug.
 */
fun DrugResultDto.toDomain(): Drug {
    val name = openFda?.brandName?.firstOrNull() ?: ""
    val manufacturer = openFda?.manufacturerName?.firstOrNull() ?: ""
    val descriptionText = description?.firstOrNull()
        ?: indicationsAndUsage?.firstOrNull()
        ?: ""

    return Drug(
        id = id,
        name = name,
        manufacturer = manufacturer,
        price = PRICE_ZERO,
        description = descriptionText
    )
}

/** DrugEntity (локальная БД) в Drug. */
fun DrugEntity.toDomain(): Drug = Drug(
    id = id,
    name = name,
    manufacturer = "",
    price = PRICE_ZERO,
    description = ""
)

