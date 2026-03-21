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
import com.example.mobile.di.AppContainer
import com.example.mobile.presentation.bookmarks.BookmarkDetailFragment
import com.example.mobile.presentation.bookmarks.BookmarksFragment
import com.example.mobile.presentation.drug.DrugDetailFragment
import com.example.mobile.presentation.history.HistoryFragment
import com.example.mobile.presentation.profile.ProfileFragment
import com.example.mobile.presentation.search.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Главная Activity приложения. Управляет нижней навигацией и переключением фрагментов.
 * Содержит логику отображения карточек препаратов и деталей закладок в back stack.
 * Координирует передачу препарата из поиска в закладки при добавлении.
 */
class MainActivity : AppCompatActivity() {

    /** Препарат, ожидающий добавления в папку закладок (выбор папки на экране BookmarksFragment) */
    private var pendingDrugForBookmark: Drug? = null

    /** Четыре основных фрагмента, добавляются при первом создании и переключаются hide/show */
    private lateinit var searchFragment: SearchFragment
    private lateinit var bookmarksFragment: BookmarksFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var profileFragment: ProfileFragment

    /** Текущий видимый фрагмент из четырёх вкладок */
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext) // applicationContext для хранилищ
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Создание фрагментов один раз, при повороте — восстановление по тегам
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
            // Сброс back stack при смене вкладки
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.navigation_search -> {
                    showFragment(searchFragment, TAG_SEARCH)
                    true
                }
                R.id.navigation_bookmarks -> {
                    showFragment(bookmarksFragment, TAG_BOOKMARKS)
                    bookmarksFragment.refreshBookmarks()
                    true
                }
                R.id.navigation_history -> {
                    showFragment(historyFragment, TAG_HISTORY)
                    historyFragment.refreshHistory() // актуальный список с сервера/файла
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

    /** Показать один фрагмент, скрыть остальные. Без анимации. */
    private fun showFragment(fragment: Fragment, tag: String) {
        if (currentFragment == fragment) return
        supportFragmentManager.beginTransaction().apply {
            currentFragment?.let { hide(it) }
            show(fragment)
            commit()
        }
        currentFragment = fragment
    }

    // После смены аккаунта в профиле
    fun refreshBookmarksAndHistory() {
        bookmarksFragment.refreshBookmarks()
        historyFragment.refreshHistory()
    }

    // Закладки: сохранить препарат до выбора папки
    fun requestAddDrugToBookmark(drug: Drug) {
        pendingDrugForBookmark = drug
        showFragment(bookmarksFragment, TAG_BOOKMARKS)
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_bookmarks
    }

    /** Возвращает препарат, ожидающий добавления, без очистки */
    fun getPendingDrugForBookmark(): Drug? = pendingDrugForBookmark

    /** Забирает препарат и обнуляет. Вызывается после добавления в папку. */
    fun getAndClearPendingDrugForBookmark(): Drug? =
        pendingDrugForBookmark.also { pendingDrugForBookmark = null }

    /** Переключение на вкладку поиска после добавления препарата в закладку */
    fun switchToSearch() {
        showFragment(searchFragment, TAG_SEARCH)
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.navigation_search
    }

    // Карточка в back stack поверх текущего фрагмента
    fun openDrugDetail(drug: Drug, fragmentToHide: Fragment? = null) {
        val toHide = fragmentToHide ?: currentFragment!!
        val detail = DrugDetailFragment.newInstance(drug)
        supportFragmentManager.beginTransaction()
            .hide(toHide)
            .add(R.id.fragment_container, detail, "detail")
            .addToBackStack(null)
            .commit()
    }

    // Список препаратов выбранной папки
    fun openBookmarkDetail(bookmark: Bookmark) {
        val detail = BookmarkDetailFragment.newInstance(bookmark)
        supportFragmentManager.beginTransaction()
            .hide(bookmarksFragment)
            .add(R.id.fragment_container, detail, "bookmark_detail")
            .addToBackStack(null)
            .commit()
    }

    /** Убирает ripple-эффект с пунктов нижней навигации */
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
