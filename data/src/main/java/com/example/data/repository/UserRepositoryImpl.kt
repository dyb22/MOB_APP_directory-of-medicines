package com.example.data.repository

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Реализация UserRepository через Firebase Auth.
 * Логин и регистрация по email/паролю. displayName или email до @ — имя пользователя.
 */
class UserRepositoryImpl : UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        val name = firebaseUser.displayName
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")
            ?: "Пользователь"
        return User(id = firebaseUser.uid, name = name)
    }

    override suspend fun login(email: String, password: String): User =
        // signInWithEmailAndPassword на IO
        withContext(Dispatchers.IO) {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            getCurrentUser() ?: error("Не удалось получить пользователя после входа")
        }

    override suspend fun register(email: String, password: String): User =
        // createUserWithEmailAndPassword на IO
        withContext(Dispatchers.IO) {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            getCurrentUser() ?: error("Не удалось получить пользователя после регистрации")
        }

    override suspend fun logout() {
        auth.signOut()
    }
}
