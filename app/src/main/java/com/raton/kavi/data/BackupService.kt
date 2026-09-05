package com.raton.kavi.data

import android.util.Base64
import androidx.room.withTransaction
import com.raton.kavi.domain.BackupCardDTO
import com.raton.kavi.domain.BackupDeckDTO
import com.raton.kavi.domain.BackupEnvelope
import com.raton.kavi.domain.BackupFolderDTO
import com.raton.kavi.domain.BackupScope
import com.raton.kavi.domain.DeckTestConfiguration
import java.nio.charset.StandardCharsets
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(envelope: BackupEnvelope): String = json.encodeToString(
        envelope.copy(schemaVersion = BackupEnvelope.CURRENT_SCHEMA_VERSION)
    )

    fun decode(raw: String): BackupEnvelope {
        val root = json.parseToJsonElement(raw).jsonObject
        val version = root["schemaVersion"]?.jsonPrimitive?.int ?: error("Missing schemaVersion")
        require(version == 1 || version == BackupEnvelope.CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schema: $version"
        }
        val decoded = json.decodeFromString<BackupEnvelope>(raw)
        require(decoded.folders.isNotEmpty() || decoded.decks.isNotEmpty()) { "Empty backup" }
        return decoded.copy(schemaVersion = BackupEnvelope.CURRENT_SCHEMA_VERSION)
    }
}

data class BackupImportReport(
    val addedFolders: Int,
    val updatedFolders: Int,
    val addedDecks: Int,
    val updatedDecks: Int,
    val addedCards: Int,
    val updatedCards: Int
)

class BackupService(private val database: KaviDatabase) {
    suspend fun databaseEnvelope(): BackupEnvelope = database.withTransaction {
        val folders = database.folderDao().getAll()
        val decks = database.deckDao().getAll()
        val cards = database.cardDao().getAll()
        BackupEnvelope(
            exportedAt = LibraryRepository.nowIso8601(),
            scope = BackupScope.database,
            folders = folders.map { folder ->
                BackupFolderDTO(
                    id = folder.id,
                    name = folder.name,
                    createdAt = folder.createdAt,
                    iconName = folder.iconName,
                    colorHex = folder.colorHex,
                    sortOrder = folder.sortOrder
                )
            },
            decks = decks.map { deck ->
                BackupDeckDTO(
                    id = deck.id,
                    name = deck.name,
                    createdAt = deck.createdAt,
                    updatedAt = deck.updatedAt,
                    lastOpenedAt = deck.lastOpenedAt,
                    completedStudySessions = deck.completedStudySessions,
                    activeStudySessionData = DataBridge.encode(deck.activeStudySessionData),
                    completedTestSessions = deck.completedTestSessions,
                    activeTestSessionData = DataBridge.encode(deck.activeTestSessionData),
                    studyHistoryData = DataBridge.encode(deck.studyHistoryData),
                    lastStudyActivityAt = deck.lastStudyActivityAt,
                    lastTestActivityAt = deck.lastTestActivityAt,
                    isPinned = deck.isPinned,
                    folderID = deck.folderId,
                    cards = cards.filter { it.deckId == deck.id }.sortedBy { it.position }.map { card ->
                        BackupCardDTO(
                            id = card.id,
                            term = card.term,
                            definition = card.definition,
                            position = card.position,
                            mastered = card.mastered,
                            testMastered = card.testMastered,
                            timesStudied = card.timesStudied,
                            timesCorrect = card.timesCorrect,
                            isStarred = card.isStarred
                        )
                    },
                    testConfiguration = TestConfigurationCodec.decode(deck.testConfigurationData)
                )
            }
        )
    }

    suspend fun importEnvelope(envelope: BackupEnvelope): BackupImportReport = database.withTransaction {
        val folderDao = database.folderDao()
        val deckDao = database.deckDao()
        val cardDao = database.cardDao()
        val existingFolders = folderDao.getAll().associateBy { it.id }.toMutableMap()
        val existingDecks = deckDao.getAll().associateBy { it.id }.toMutableMap()
        val existingCards = cardDao.getAll().associateBy { it.id }.toMutableMap()
        var addedFolders = 0
        var updatedFolders = 0
        var addedDecks = 0
        var updatedDecks = 0
        var addedCards = 0
        var updatedCards = 0

        envelope.folders.forEach { dto ->
            val current = existingFolders[dto.id]
            val next = FolderEntity(dto.id, dto.name, dto.createdAt, dto.iconName, dto.colorHex, dto.sortOrder)
            if (current == null) {
                folderDao.insert(next)
                addedFolders++
            } else {
                folderDao.update(next)
                updatedFolders++
            }
            existingFolders[dto.id] = next
        }

        envelope.decks.forEach { dto ->
            val current = existingDecks[dto.id]
            val currentConfig = current?.let { TestConfigurationCodec.decode(it.testConfigurationData) }
                ?: DeckTestConfiguration.useFlashcards
            val shell = DeckEntity(
                id = dto.id,
                name = dto.name,
                deckDescription = current?.deckDescription,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                lastOpenedAt = dto.lastOpenedAt ?: current?.lastOpenedAt,
                completedStudySessions = dto.completedStudySessions,
                activeStudySessionData = DataBridge.decode(dto.activeStudySessionData),
                completedTestSessions = dto.completedTestSessions,
                activeTestSessionData = DataBridge.decode(dto.activeTestSessionData),
                studyHistoryData = DataBridge.decode(dto.studyHistoryData),
                lastStudyActivityAt = dto.lastStudyActivityAt,
                lastTestActivityAt = dto.lastTestActivityAt,
                isPinned = dto.isPinned,
                testConfigurationData = current?.testConfigurationData,
                folderId = dto.folderID?.takeIf(existingFolders::containsKey)
            )
            if (current == null) {
                deckDao.insert(shell)
                addedDecks++
            } else {
                deckDao.update(shell)
                updatedDecks++
            }
            existingDecks[dto.id] = shell

            dto.cards.forEach { cardDTO ->
                val card = CardEntity(
                    id = cardDTO.id,
                    term = cardDTO.term,
                    definition = cardDTO.definition,
                    position = cardDTO.position,
                    mastered = cardDTO.mastered,
                    testMastered = cardDTO.testMastered,
                    timesStudied = cardDTO.timesStudied,
                    timesCorrect = cardDTO.timesCorrect,
                    isStarred = cardDTO.isStarred,
                    deckId = dto.id
                )
                if (existingCards[card.id] == null) {
                    cardDao.insert(card)
                    addedCards++
                } else {
                    cardDao.update(card)
                    updatedCards++
                }
                existingCards[card.id] = card
            }

            val validCardIDs = existingCards.values.filter { it.deckId == dto.id }.mapTo(mutableSetOf()) { it.id }
            val merged = currentConfig.mergingQuestions(dto.testConfiguration).validated(validCardIDs)
            val finalDeck = shell.copy(testConfigurationData = TestConfigurationCodec.encodeOrNull(merged))
            deckDao.update(finalDeck)
            existingDecks[dto.id] = finalDeck
        }

        BackupImportReport(
            addedFolders,
            updatedFolders,
            addedDecks,
            updatedDecks,
            addedCards,
            updatedCards
        )
    }
}

private object DataBridge {
    fun encode(json: String?): String? = json?.let {
        Base64.encodeToString(it.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    fun decode(base64: String?): String? = base64?.let {
        String(Base64.decode(it, Base64.DEFAULT), StandardCharsets.UTF_8)
    }
}
