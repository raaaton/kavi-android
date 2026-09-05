package com.raton.kavi.domain

import kotlinx.serialization.Serializable

@Serializable
enum class BackupScope { deck, database }

@Serializable
data class BackupFolderDTO(
    val id: String,
    val name: String,
    val createdAt: String,
    val iconName: String = "folder.fill",
    val colorHex: String = "5856D6",
    val sortOrder: Int = Int.MAX_VALUE
)

@Serializable
data class BackupCardDTO(
    val id: String,
    val term: String,
    val definition: String,
    val position: Int,
    val mastered: Boolean,
    val testMastered: Boolean = false,
    val timesStudied: Int,
    val timesCorrect: Int,
    val isStarred: Boolean = false
)

@Serializable
data class BackupDeckDTO(
    val id: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val lastOpenedAt: String? = null,
    val completedStudySessions: Int = 0,
    val activeStudySessionData: String? = null,
    val completedTestSessions: Int = 0,
    val activeTestSessionData: String? = null,
    val studyHistoryData: String? = null,
    val lastStudyActivityAt: String? = null,
    val lastTestActivityAt: String? = null,
    val isPinned: Boolean = false,
    val folderID: String? = null,
    val cards: List<BackupCardDTO>,
    val testConfiguration: DeckTestConfiguration = DeckTestConfiguration.useFlashcards
)

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: String,
    val scope: BackupScope,
    val folders: List<BackupFolderDTO>,
    val decks: List<BackupDeckDTO>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
