package com.example.mobile.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobile.R

class ProfileFragment : Fragment() {

    private var loggedIn = false
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderState(view as FrameLayout)
    }

    private fun renderState(root: FrameLayout) {
        root.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (!loggedIn) {
            val loginView = inflater.inflate(R.layout.view_profile_login, root, false)
            val emailInput = loginView.findViewById<EditText>(R.id.input_email)
            val passwordInput = loginView.findViewById<EditText>(R.id.input_password)
            val loginButton = loginView.findViewById<Button>(R.id.button_login)

            loginButton.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    loggedIn = true
                    userEmail = email
                    renderState(root)
                }
            }

            root.addView(loginView)
        } else {
            val profileView = inflater.inflate(R.layout.view_profile_info, root, false)
            val userNameText = profileView.findViewById<TextView>(R.id.text_user_name)
            val logoutButton = profileView.findViewById<Button>(R.id.button_logout)

            val fromEmail = userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
            userNameText.text = fromEmail ?: getString(R.string.profile_user_name_example)

            logoutButton.setOnClickListener {
                loggedIn = false
                userEmail = null
                renderState(root)
            }

            root.addView(profileView)
        }
    }
}

