package com.raton.kavi

import android.app.Application
import com.raton.kavi.data.KaviDatabase
import com.raton.kavi.data.LibraryRepository
import com.raton.kavi.data.PreferencesRepository

class KaviApplication : Application() {
    val database by lazy { KaviDatabase.create(this) }
    val libraryRepository by lazy { LibraryRepository(database) }
    val preferencesRepository by lazy { PreferencesRepository(this) }
}
