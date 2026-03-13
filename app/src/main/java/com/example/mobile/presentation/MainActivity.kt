package com.example.mobile.presentation

import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.domain.model.Bookmark
import com.example.domain.model.Drug
import com.example.mobile.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var pendingDrugForBookmark: Drug? = null

    private lateinit var searchFragment: SearchFragment
    private lateinit var bookmarksFragment: BookmarksFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var profileFragment: ProfileFragment

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            searchFragment = SearchFragment()
            bookmarksFragment = BookmarksFragment()
            historyFragment = HistoryFragment()
            profileFragment = ProfileFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, searchFragment, TAG_SEARCH)
                .add(R.id.fragment_container, bookmarksFragment, TAG_BOOKMARKS)
                .add(R.id.fragment_container, historyFragment, TAG_HISTORY)
                .add(R.id.fragment_container, profileFragment, TAG_PROFILE)
                .hide(bookmarksFragment)
                .hide(historyFragment)
                .hide(profileFragment)
                .commit()
            currentFragment = searchFragment
        } else {
            searchFragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH) as SearchFragment
            bookmarksFragment = supportFragmentManager.findFragmentByTag(TAG_BOOKMARKS) as BookmarksFragment
            historyFragment = supportFragmentManager.findFragmentByTag(TAG_HISTORY) as HistoryFragment
            profileFragment = supportFragmentManager.findFragmentByTag(TAG_PROFILE) as ProfileFragment
            currentFragment = searchFragment
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            // Закрываем экран открытой закладки/карточки при переключении вкладки
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.navigation_search -> {
                    showFragment(searchFragment, TAG_SEARCH)
                    true
                }
                R.id.navigation_bookmarks -> {
                    showFragment(bookmarksFragment, TAG_BOOKMARKS)
                    true
                }
                R.id.navigation_history -> {
                    showFragment(historyFragment, TAG_HISTORY)
                    // При каждом переходе на вкладку истории явно обновляем список
                    historyFragment.refreshHistory()
                    true
                }
                R.id.navigation_profile -> {
                    showFragment(profileFragment, TAG_PROFILE)
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_search
        }

        bottomNavigation.post { removeItemRipple(bottomNavigation) }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        if (currentFragment == fragment) return
        supportFragmentManager.beginTransaction().apply {
            currentFragment?.let { hide(it) }
            show(fragment)
            commit()
        }
        currentFragment = fragment
    }

    /** Вызвать с экрана поиска: перейти на закладки и запомнить препарат для добавления. */
    fun requestAddDrugToBookmark(drug: Drug) {
        pendingDrugForBookmark = drug
        showFragment(bookmarksFragment, TAG_BOOKMARKS)
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_bookmarks
    }

    /** Текущий препарат, ожидающий добавления в закладку (без очистки). */
    fun getPendingDrugForBookmark(): Drug? = pendingDrugForBookmark

    /** Забрать и обнулить препарат для добавления в закладку. */
    fun getAndClearPendingDrugForBookmark(): Drug? =
        pendingDrugForBookmark.also { pendingDrugForBookmark = null }

    /** Вернуться на экран поиска (после добавления препарата в закладку). */
    fun switchToSearch() {
        showFragment(searchFragment, TAG_SEARCH)
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_search
    }

    /** Открыть карточку препарата поверх текущего экрана. fragmentToHide — фрагмент поверх которого открываем (например BookmarkDetailFragment). */
    fun openDrugDetail(drug: Drug, fragmentToHide: Fragment? = null) {
        val toHide = fragmentToHide ?: currentFragment!!
        val detail = DrugDetailFragment.newInstance(drug)
        supportFragmentManager.beginTransaction()
            .hide(toHide)
            .add(R.id.fragment_container, detail, "detail")
            .addToBackStack(null)
            .commit()
    }

    /** Открыть содержимое закладки (список препаратов). */
    fun openBookmarkDetail(bookmark: Bookmark) {
        val detail = BookmarkDetailFragment.newInstance(bookmark)
        supportFragmentManager.beginTransaction()
            .hide(bookmarksFragment)
            .add(R.id.fragment_container, detail, "bookmark_detail")
            .addToBackStack(null)
            .commit()
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

    companion object {
        private const val TAG_SEARCH = "search"
        private const val TAG_BOOKMARKS = "bookmarks"
        private const val TAG_HISTORY = "history"
        private const val TAG_PROFILE = "profile"
    }
}
