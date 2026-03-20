package com.example.mobile.di

import android.content.Context
import com.example.data.repository.DrugRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.repository.DrugRepository
import com.example.domain.repository.UserRepository

/**
 * Единые экземпляры репозиториев (закладки/история не пропадают при переключении экранов;
 * пользователь — Firebase Auth).
 */
object AppContainer {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val userRepository: UserRepository by lazy { UserRepositoryImpl() }
    val drugRepository: DrugRepository by lazy { DrugRepositoryImpl(appContext) }
}
