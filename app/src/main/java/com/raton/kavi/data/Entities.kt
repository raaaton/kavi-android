package com.raton.kavi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: String,
    val iconName: String = "folder.fill",
    val colorHex: String = "5856D6",
    val sortOrder: Int = Int.MAX_VALUE
)

@Entity(
    tableName = "decks",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class DeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deckDescription: String? = null,
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
    val testConfigurationData: String? = null,
    val folderId: String? = null
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId")]
)
data class CardEntity(
    @PrimaryKey val id: String,
    val term: String,
    val definition: String,
    val position: Int,
    val mastered: Boolean = false,
    val testMastered: Boolean = false,
    val timesStudied: Int = 0,
    val timesCorrect: Int = 0,
    val isStarred: Boolean = false,
    val deckId: String
)

data class LibrarySnapshot(
    val folders: List<FolderEntity>,
    val decks: List<DeckEntity>,
    val cards: List<CardEntity>
) {
    fun decksIn(folderId: String?): List<DeckEntity> = decks.filter { it.folderId == folderId }
    fun cardsIn(deckId: String): List<CardEntity> = cards.filter { it.deckId == deckId }.sortedBy { it.position }
    fun folderCount(folderId: String): Int = decks.count { it.folderId == folderId }
    fun cardCount(deckId: String): Int = cards.count { it.deckId == deckId }
}
