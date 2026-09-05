package com.raton.kavi.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raton.kavi.domain.StudyDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.kaviPreferences by preferencesDataStore(name = "kavi_settings")

enum class AppLanguage { automatic, french, english, german, spanish }

data class SettingsState(
    val hapticsEnabled: Boolean = true,
    val celebrationsEnabled: Boolean = true,
    val searchScopeEnabled: Boolean = true,
    val homeResumeEnabled: Boolean = true,
    val homeRecentEnabled: Boolean = true,
    val homePinnedEnabled: Boolean = true,
    val studyHistoryEnabled: Boolean = true,
    val studyDirection: StudyDirection = StudyDirection.termToDefinition,
    val studyShuffle: Boolean = true,
    val studyStarredOnly: Boolean = false,
    val language: AppLanguage = AppLanguage.automatic
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val haptics = booleanPreferencesKey("settings.hapticsEnabled")
        val celebrations = booleanPreferencesKey("settings.celebrationsEnabled")
        val searchScope = booleanPreferencesKey("settings.searchScopeEnabled")
        val homeResume = booleanPreferencesKey("settings.home.resumeEnabled")
        val homeRecent = booleanPreferencesKey("settings.home.recentEnabled")
        val homePinned = booleanPreferencesKey("settings.home.pinnedEnabled")
        val history = booleanPreferencesKey("settings.study.historyEnabled")
        val studyDirection = stringPreferencesKey("study.direction")
        val studyShuffle = booleanPreferencesKey("study.shuffle")
        val studyStarredOnly = booleanPreferencesKey("study.starredOnly")
        val language = stringPreferencesKey("settings.language")
    }

    val settings: Flow<SettingsState> = context.kaviPreferences.data.map(::decode)

    suspend fun setHaptics(value: Boolean) = set(Keys.haptics, value)
    suspend fun setCelebrations(value: Boolean) = set(Keys.celebrations, value)
    suspend fun setSearchScope(value: Boolean) = set(Keys.searchScope, value)
    suspend fun setHomeResume(value: Boolean) = set(Keys.homeResume, value)
    suspend fun setHomeRecent(value: Boolean) = set(Keys.homeRecent, value)
    suspend fun setHomePinned(value: Boolean) = set(Keys.homePinned, value)
    suspend fun setStudyHistory(value: Boolean) = set(Keys.history, value)
    suspend fun setStudyShuffle(value: Boolean) = set(Keys.studyShuffle, value)
    suspend fun setStudyStarredOnly(value: Boolean) = set(Keys.studyStarredOnly, value)

    suspend fun setStudyDirection(value: StudyDirection) {
        context.kaviPreferences.edit { it[Keys.studyDirection] = value.name }
    }

    suspend fun setLanguage(value: AppLanguage) {
        context.kaviPreferences.edit { it[Keys.language] = value.name }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.kaviPreferences.edit { it[key] = value }
    }

    private fun decode(preferences: Preferences): SettingsState = SettingsState(
        hapticsEnabled = preferences[Keys.haptics] ?: true,
        celebrationsEnabled = preferences[Keys.celebrations] ?: true,
        searchScopeEnabled = preferences[Keys.searchScope] ?: true,
        homeResumeEnabled = preferences[Keys.homeResume] ?: true,
        homeRecentEnabled = preferences[Keys.homeRecent] ?: true,
        homePinnedEnabled = preferences[Keys.homePinned] ?: true,
        studyHistoryEnabled = preferences[Keys.history] ?: true,
        studyDirection = preferences[Keys.studyDirection]
            ?.let { runCatching { StudyDirection.valueOf(it) }.getOrNull() }
            ?: StudyDirection.termToDefinition,
        studyShuffle = preferences[Keys.studyShuffle] ?: true,
        studyStarredOnly = preferences[Keys.studyStarredOnly] ?: false,
        language = preferences[Keys.language]
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.automatic
    )
}
