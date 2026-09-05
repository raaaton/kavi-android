package com.raton.kavi.data

import androidx.room.withTransaction
import com.raton.kavi.domain.DeckTestConfiguration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LibraryRepository(private val database: KaviDatabase) {
    private val folders = database.folderDao()
    private val decks = database.deckDao()
    private val cards = database.cardDao()

    val library: Flow<LibrarySnapshot> = combine(
        folders.observeAll(),
        decks.observeAll(),
        cards.observeAll()
    ) { folderList, deckList, cardList ->
        LibrarySnapshot(folderList, deckList, cardList)
    }

    suspend fun createFolder(name: String): String {
        val clean = name.trim()
        require(clean.isNotEmpty())
        val id = newUUID()
        val nextOrder = (folders.maxSortOrder() ?: -1).coerceAtMost(Int.MAX_VALUE - 1) + 1
        folders.insert(
            FolderEntity(
                id = id,
                name = clean,
                createdAt = nowIso8601(),
                sortOrder = nextOrder
            )
        )
        return id
    }

    suspend fun renameFolder(id: String, name: String) {
        val current = folders.getAll().firstOrNull { it.id == id } ?: return
        val clean = name.trim()
        require(clean.isNotEmpty())
        folders.update(current.copy(name = clean))
    }

    suspend fun moveFolder(id: String, delta: Int) = database.withTransaction {
        val ordered = folders.getAll().toMutableList()
        val from = ordered.indexOfFirst { it.id == id }
        if (from == -1) return@withTransaction
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from == to) return@withTransaction
        val item = ordered.removeAt(from)
        ordered.add(to, item)
        ordered.forEachIndexed { index, folder ->
            if (folder.sortOrder != index) folders.update(folder.copy(sortOrder = index))
        }
    }

    suspend fun deleteFolder(id: String, keepDecks: Boolean) = database.withTransaction {
        if (keepDecks) decks.clearFolder(id, nowIso8601())
        folders.deleteById(id)
    }

    suspend fun createDeck(name: String, folderId: String? = null): String {
        val clean = name.trim()
        require(clean.isNotEmpty())
        val id = newUUID()
        val now = nowIso8601()
        decks.insert(
            DeckEntity(
                id = id,
                name = clean,
                createdAt = now,
                updatedAt = now,
                folderId = folderId
            )
        )
        return id
    }

    suspend fun renameDeck(id: String, name: String) {
        val deck = decks.getById(id) ?: return
        val clean = name.trim()
        require(clean.isNotEmpty())
        decks.update(deck.copy(name = clean, updatedAt = nowIso8601()))
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        val deck = decks.getById(id) ?: return
        decks.update(deck.copy(isPinned = pinned, updatedAt = nowIso8601()))
    }

    suspend fun markOpened(id: String) {
        val deck = decks.getById(id) ?: return
        decks.update(deck.copy(lastOpenedAt = nowIso8601()))
    }

    suspend fun deleteDeck(id: String) {
        decks.deleteById(id)
    }

    suspend fun duplicateDeck(id: String): String? = database.withTransaction {
        val source = decks.getById(id) ?: return@withTransaction null
        val sourceCards = cards.getForDeck(id)
        val newDeckId = newUUID()
        val now = nowIso8601()
        val cardIDMap = sourceCards.associate { it.id to newUUID() }
        val config = TestConfigurationCodec.decode(source.testConfigurationData)
            .duplicated(cardIDMap, ::newUUID)
        decks.insert(
            source.copy(
                id = newDeckId,
                name = "${source.name} — Copy",
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = null,
                completedStudySessions = 0,
                activeStudySessionData = null,
                completedTestSessions = 0,
                activeTestSessionData = null,
                studyHistoryData = null,
                lastStudyActivityAt = null,
                lastTestActivityAt = null,
                isPinned = false,
                testConfigurationData = TestConfigurationCodec.encodeOrNull(config)
            )
        )
        sourceCards.sortedBy { it.position }.forEach { card ->
            cards.insert(
                card.copy(
                    id = cardIDMap.getValue(card.id),
                    deckId = newDeckId,
                    mastered = false,
                    testMastered = false,
                    timesStudied = 0,
                    timesCorrect = 0
                )
            )
        }
        newDeckId
    }

    suspend fun createCard(deckId: String, term: String, definition: String): String = database.withTransaction {
        val cleanTerm = term.trim()
        val cleanDefinition = definition.trim()
        require(cleanTerm.isNotEmpty() && cleanDefinition.isNotEmpty())
        val deck = decks.getById(deckId) ?: error("Deck not found")
        val deckCards = cards.getForDeck(deckId)
        val id = newUUID()
        cards.insert(
            CardEntity(
                id = id,
                term = cleanTerm,
                definition = cleanDefinition,
                position = deckCards.size,
                deckId = deckId
            )
        )
        decks.update(deck.copy(updatedAt = nowIso8601()))
        id
    }

    suspend fun updateCard(id: String, term: String, definition: String) = database.withTransaction {
        val cleanTerm = term.trim()
        val cleanDefinition = definition.trim()
        require(cleanTerm.isNotEmpty() && cleanDefinition.isNotEmpty())
        val (card, deck) = cardAndDeck(id) ?: return@withTransaction
        cards.update(card.copy(term = cleanTerm, definition = cleanDefinition))
        decks.update(deck.copy(updatedAt = nowIso8601()))
    }

    suspend fun setStarred(id: String, starred: Boolean) = database.withTransaction {
        val (card, deck) = cardAndDeck(id) ?: return@withTransaction
        cards.update(card.copy(isStarred = starred))
        decks.update(deck.copy(updatedAt = nowIso8601()))
    }

    suspend fun moveCard(id: String, delta: Int) = database.withTransaction {
        val (card, deck) = cardAndDeck(id) ?: return@withTransaction
        val ordered = cards.getForDeck(card.deckId).toMutableList()
        val from = ordered.indexOfFirst { it.id == id }
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from == to) return@withTransaction
        val item = ordered.removeAt(from)
        ordered.add(to, item)
        ordered.forEachIndexed { index, entry ->
            if (entry.position != index) cards.update(entry.copy(position = index))
        }
        decks.update(deck.copy(updatedAt = nowIso8601()))
    }

    suspend fun deleteCard(id: String) = database.withTransaction {
        val (card, deck) = cardAndDeck(id) ?: return@withTransaction
        val config = TestConfigurationCodec.decode(deck.testConfigurationData)
            .removingQuestions(setOf(id))
        cards.deleteById(id)
        cards.getForDeck(card.deckId).forEachIndexed { index, entry ->
            if (entry.position != index) cards.update(entry.copy(position = index))
        }
        decks.update(
            deck.copy(
                updatedAt = nowIso8601(),
                testConfigurationData = TestConfigurationCodec.encodeOrNull(config)
            )
        )
    }

    private suspend fun cardAndDeck(id: String): Pair<CardEntity, DeckEntity>? {
        val card = cards.getById(id) ?: return null
        val deck = decks.getById(card.deckId) ?: return null
        return card to deck
    }

    companion object {
        fun newUUID(): String = UUID.randomUUID().toString()

        fun nowIso8601(): String {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.format(Date())
        }
    }
}

object TestConfigurationCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun decode(value: String?): DeckTestConfiguration = value
        ?.let { runCatching { json.decodeFromString<DeckTestConfiguration>(it) }.getOrNull() }
        ?: DeckTestConfiguration.useFlashcards

    fun encodeOrNull(configuration: DeckTestConfiguration): String? =
        if (configuration == DeckTestConfiguration.useFlashcards) null else json.encodeToString(configuration)
}
