package com.example.mobile.di

import com.example.data.repository.DrugRepositoryImpl
import com.example.domain.repository.DrugRepository

/**
 * Единый экземпляр репозитория, чтобы закладки не пропадали при переключении экранов.
 */
object AppContainer {
    val drugRepository: DrugRepository by lazy { DrugRepositoryImpl() }
}
