package com.example.mobile.di

import android.content.Context
import com.example.data.repository.DrugRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.repository.DrugRepository
import com.example.domain.repository.UserRepository

/**
 * Контейнер зависимостей приложения. Хранит единственные экземпляры репозиториев.
 * Репозитории создаются лениво, не пропадают при переключении экранов.
 */
object AppContainer {
    private lateinit var appContext: Context

    /** Инициализация до первого обращения к репозиториям */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Репозиторий пользователя: Firebase Auth (вход, регистрация, выход) */
    val userRepository: UserRepository by lazy { UserRepositoryImpl() }

    /** Репозиторий препаратов: OpenFDA, Firestore, локальный гость */
    val drugRepository: DrugRepository by lazy { DrugRepositoryImpl(appContext) }
}
