package com.raton.kavi.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SessionCodecs {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeStudy(snapshot: ActiveStudySessionSnapshot): String = json.encodeToString(snapshot)

    fun decodeStudy(raw: String?, deckID: String): ActiveStudySessionSnapshot? {
        val snapshot = raw?.let {
            runCatching { json.decodeFromString<ActiveStudySessionSnapshot>(it) }.getOrNull()
        } ?: return null
        return snapshot.takeIf { it.deckID == deckID && !it.state.isComplete }
    }

    fun canResumeStudy(raw: String?, deckID: String, existingCardIDs: Set<String>): Boolean {
        val snapshot = decodeStudy(raw, deckID) ?: return false
        val state = snapshot.state
        if (state.currentIndex <= 0 || state.currentIndex >= state.items.size) return false
        return state.items.drop(state.currentIndex).all { it.id in existingCardIDs }
    }

    fun encodeTest(snapshot: ActiveTestSessionSnapshot): String = json.encodeToString(snapshot)

    fun decodeTest(raw: String?, deckID: String): ActiveTestSessionSnapshot? {
        val snapshot = raw?.let {
            runCatching { json.decodeFromString<ActiveTestSessionSnapshot>(it) }.getOrNull()
        } ?: return null
        return snapshot.takeIf {
            it.deckID == deckID && !it.state.isComplete &&
                (it.state.currentIndex > 0 || it.state.answers.isNotEmpty())
        }
    }
}
