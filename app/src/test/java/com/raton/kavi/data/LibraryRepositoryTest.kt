package com.raton.kavi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRepositoryTest {
    private lateinit var database: KaviDatabase
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KaviDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LibraryRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingFolderCanKeepDecksAndCardsInUnfiled() = runBlocking {
        val folderID = repository.createFolder("History")
        val deckID = repository.createDeck("1929", folderID)
        val cardID = repository.createCard(deckID, "Crash", "Wall Street")

        repository.deleteFolder(folderID, keepDecks = true)
        val snapshot = repository.library.first()

        assertFalse(snapshot.folders.any { it.id == folderID })
        assertEquals(null, snapshot.decks.first { it.id == deckID }.folderId)
        assertTrue(snapshot.cards.any { it.id == cardID && it.deckId == deckID })
    }

    @Test
    fun deletingFolderWithContentsCascadesThroughDecksAndCards() = runBlocking {
        val folderID = repository.createFolder("Math")
        val deckID = repository.createDeck("Calculus", folderID)
        repository.createCard(deckID, "d/dx x²", "2x")

        repository.deleteFolder(folderID, keepDecks = false)
        val snapshot = repository.library.first()

        assertFalse(snapshot.decks.any { it.id == deckID })
        assertFalse(snapshot.cards.any { it.deckId == deckID })
    }

    @Test
    fun cardMovesAndDeletesAlwaysNormalizePositions() = runBlocking {
        val deckID = repository.createDeck("Order")
        val first = repository.createCard(deckID, "A", "1")
        val second = repository.createCard(deckID, "B", "2")
        val third = repository.createCard(deckID, "C", "3")

        repository.moveCard(third, -2)
        var cards = repository.library.first().cardsIn(deckID)
        assertEquals(listOf(third, first, second), cards.map { it.id })
        assertEquals(listOf(0, 1, 2), cards.map { it.position })

        repository.deleteCard(first)
        cards = repository.library.first().cardsIn(deckID)
        assertEquals(listOf(third, second), cards.map { it.id })
        assertEquals(listOf(0, 1), cards.map { it.position })
    }

    @Test
    fun duplicateDeckGetsFreshStableIdsAndKeepsStarState() = runBlocking {
        val deckID = repository.createDeck("Biology")
        val cardID = repository.createCard(deckID, "Cell", "Basic unit of life")
        repository.setStarred(cardID, true)

        val duplicateID = repository.duplicateDeck(deckID)
        assertNotNull(duplicateID)
        UUID.fromString(duplicateID!!)
        val snapshot = repository.library.first()
        val sourceCard = snapshot.cardsIn(deckID).single()
        val duplicateCard = snapshot.cardsIn(duplicateID).single()

        assertNotEquals(deckID, duplicateID)
        assertNotEquals(sourceCard.id, duplicateCard.id)
        UUID.fromString(duplicateCard.id)
        assertTrue(duplicateCard.isStarred)
        assertFalse(duplicateCard.mastered)
        assertEquals(0, duplicateCard.timesStudied)
    }
}
