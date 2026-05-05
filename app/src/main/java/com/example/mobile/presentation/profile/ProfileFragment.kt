package com.example.mobile.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobile.BuildConfig
import com.example.mobile.R
import com.example.mobile.di.AppContainer
import com.example.mobile.presentation.MainActivity
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

/**
 * Экран профиля. Гость: форма входа и регистрации. Авторизован: имя и кнопка выхода.
 * renderState пересобирает корневой контейнер. После выхода — refreshBookmarksAndHistory в MainActivity.
 */
class ProfileFragment : Fragment() {

    private var loggedIn = false
    private var userName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val root = view as FrameLayout
        viewLifecycleOwner.lifecycleScope.launch {
            // проверить текущего пользователя и отрисовать форму или профиль
            val user = AppContainer.userRepository.getCurrentUser()
            loggedIn = user != null
            userName = user?.name
            renderState(root)
        }
    }

    // Экран входа или профиля с нуля
    private fun renderState(root: FrameLayout) {
        root.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (!loggedIn) {
            val loginView = inflater.inflate(R.layout.view_profile_login, root, false)
            val emailInput = loginView.findViewById<EditText>(R.id.input_email)
            val passwordInput = loginView.findViewById<EditText>(R.id.input_password)
            val loginButton = loginView.findViewById<Button>(R.id.button_login)
            val registerButton = loginView.findViewById<Button>(R.id.button_register)
            val authErrorText = loginView.findViewById<TextView>(R.id.text_auth_error)

            fun addAuthLoadingOverlay(): View {
                // оверлей поверх формы при отправке логина/регистрации
                val loadingView = inflater.inflate(R.layout.view_loading_overlay, root, false)
                root.addView(loadingView)
                loadingView.bringToFront()
                return loadingView
            }

            fun removeAuthLoadingOverlay(loadingView: View) {
                if (loadingView.parent == root) root.removeView(loadingView)
            }

            fun showAuthError(errorResId: Int) {
                // красный текст ошибки под полями
                authErrorText.setText(errorResId)
                authErrorText.visibility = View.VISIBLE
            }

            authErrorText.visibility = View.GONE

            loginButton.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.profile_login_instruction, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                authErrorText.visibility = View.GONE
                loginButton.isEnabled = false
                registerButton?.isEnabled = false
                val loadingView = addAuthLoadingOverlay()

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val user = AppContainer.userRepository.login(email, password)
                        loggedIn = true
                        userName = user.name
                        removeAuthLoadingOverlay(loadingView)
                        renderState(root)
                    } catch (e: Exception) {
                        val errorRes = when (e) {
                            is FirebaseAuthInvalidUserException,
                            is FirebaseAuthInvalidCredentialsException -> R.string.profile_auth_error_invalid_credentials

                            else -> null
                        }

                        if (errorRes != null) {
                            showAuthError(errorRes)
                        } else {
                            Toast.makeText(requireContext(), e.message ?: "Ошибка входа", Toast.LENGTH_SHORT).show()
                        }
                        removeAuthLoadingOverlay(loadingView)
                    }
                    loginButton.isEnabled = true
                    registerButton?.isEnabled = true
                }
            }

            registerButton?.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.profile_login_instruction, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                authErrorText.visibility = View.GONE
                loginButton.isEnabled = false
                registerButton.isEnabled = false
                val loadingView = addAuthLoadingOverlay()

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val user = AppContainer.userRepository.register(email, password)
                        loggedIn = true
                        userName = user.name
                        removeAuthLoadingOverlay(loadingView)
                        renderState(root)
                    } catch (e: Exception) {
                        // Текст входа (про «нет пользователя») сюда не подставляем — при регистрации Firebase
                        // часто шлёт InvalidCredentialsException / WeakPassword при коротком пароле или email.
                        val errorRes = when (e) {
                            is FirebaseAuthUserCollisionException -> R.string.profile_auth_error_user_already_exists
                            is FirebaseAuthWeakPasswordException -> R.string.profile_register_error_weak_password
                            is FirebaseAuthInvalidCredentialsException -> R.string.profile_register_error_invalid_input
                            else -> null
                        }

                        if (errorRes != null) {
                            showAuthError(errorRes)
                        } else {
                            Toast.makeText(requireContext(), e.message ?: "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                        }
                        removeAuthLoadingOverlay(loadingView)
                    }
                    loginButton.isEnabled = true
                    registerButton.isEnabled = true
                }
            }

            root.addView(loginView)
        } else {
            val profileView = inflater.inflate(R.layout.view_profile_info, root, false)
            val userNameText = profileView.findViewById<TextView>(R.id.text_user_name)
            val appVersionText = profileView.findViewById<TextView>(R.id.text_app_version)
            val logoutButton = profileView.findViewById<Button>(R.id.button_logout)

            userNameText.text = userName ?: getString(R.string.profile_user_name_example)
            appVersionText.text = getString(R.string.profile_app_version_format, BuildConfig.VERSION_NAME)

            logoutButton.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    AppContainer.userRepository.logout()
                    loggedIn = false
                    userName = null
                    renderState(root)
                    (activity as? MainActivity)?.refreshBookmarksAndHistory()
                }
            }

            root.addView(profileView)
        }
    }
}

