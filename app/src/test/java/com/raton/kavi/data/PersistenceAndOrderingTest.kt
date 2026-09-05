package com.raton.kavi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PersistenceAndOrderingTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun libraryStateSurvivesDatabaseCloseAndReopen() {
        runBlocking {
            val name = "kavi-persistence-review.db"
            context.deleteDatabase(name)

            var database = Room.databaseBuilder(context, KaviDatabase::class.java, name)
                .allowMainThreadQueries()
                .build()
            var repository = LibraryRepository(database)

            val folderId = repository.createFolder("Persistent folder")
            val deckId = repository.createDeck("Persistent deck", folderId)
            val firstCardId = repository.createCard(deckId, "First", "1")
            val secondCardId = repository.createCard(deckId, "Second", "2")
            repository.setPinned(deckId, true)
            repository.setStarred(secondCardId, true)
            repository.moveCard(secondCardId, -1)
            database.close()

            database = Room.databaseBuilder(context, KaviDatabase::class.java, name)
                .allowMainThreadQueries()
                .build()
            repository = LibraryRepository(database)
            val snapshot = repository.library.first()

            assertTrue(snapshot.folders.any { it.id == folderId })
            assertEquals(folderId, snapshot.decks.single { it.id == deckId }.folderId)
            assertTrue(snapshot.decks.single { it.id == deckId }.isPinned)
            assertEquals(listOf(secondCardId, firstCardId), snapshot.cardsIn(deckId).map { it.id })
            assertEquals(listOf(0, 1), snapshot.cardsIn(deckId).map { it.position })
            assertTrue(snapshot.cards.single { it.id == secondCardId }.isStarred)

            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun creationNormalizesImportedSentinelAndSparseOrders() {
        runBlocking {
            val database = Room.inMemoryDatabaseBuilder(context, KaviDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            val repository = LibraryRepository(database)
            val now = LibraryRepository.nowIso8601()

            database.folderDao().insert(
                FolderEntity(
                    id = "legacy-folder",
                    name = "Legacy",
                    createdAt = now,
                    sortOrder = Int.MAX_VALUE
                )
            )
            val newFolderId = repository.createFolder("New")
            assertEquals(listOf(0, 1), database.folderDao().getAll().map { it.sortOrder })
            assertEquals(1, database.folderDao().getAll().single { it.id == newFolderId }.sortOrder)

            database.deckDao().insert(
                DeckEntity(
                    id = "legacy-deck",
                    name = "Legacy deck",
                    createdAt = now,
                    updatedAt = now
                )
            )
            database.cardDao().insert(CardEntity("card-a", "A", "1", 10, deckId = "legacy-deck"))
            database.cardDao().insert(CardEntity("card-b", "B", "2", 50, deckId = "legacy-deck"))
            val newCardId = repository.createCard("legacy-deck", "C", "3")

            val cards = database.cardDao().getForDeck("legacy-deck")
            assertEquals(listOf(0, 1, 2), cards.map { it.position })
            assertEquals(2, cards.single { it.id == newCardId }.position)

            database.close()
        }
    }
}
