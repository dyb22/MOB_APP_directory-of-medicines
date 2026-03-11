package com.example.mobile.presentation

import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_search -> {
                    openFragment(SearchFragment())
                    true
                }
                R.id.navigation_bookmarks -> {
                    openFragment(BookmarksFragment())
                    true
                }
                R.id.navigation_history -> {
                    openFragment(HistoryFragment())
                    true
                }
                R.id.navigation_profile -> {
                    openFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_search
        }

        // Убираем стандартный ripple/фон при нажатии у пунктов нижнего меню
        bottomNavigation.post { removeItemRipple(bottomNavigation) }
    }

    private fun removeItemRipple(bottomNavigation: BottomNavigationView) {
        val menuView = bottomNavigation.getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until menuView.childCount) {
            val itemView = menuView.getChildAt(i)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                itemView.foreground = null
            }
            itemView.isFocusable = false
            itemView.clipToOutline = false
            (itemView as? ViewGroup)?.clipChildren = false
        }
        menuView.clipChildren = false
    }

    private fun openFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}