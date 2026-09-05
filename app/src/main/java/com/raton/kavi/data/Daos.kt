package com.raton.kavi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT MAX(sortOrder) FROM folders")
    suspend fun maxSortOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folder: FolderEntity)

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE folderId = :folderId ORDER BY createdAt ASC, id ASC")
    suspend fun getInFolder(folderId: String): List<DeckEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: DeckEntity)

    @Update
    suspend fun update(deck: DeckEntity)

    @Query("UPDATE decks SET folderId = NULL, updatedAt = :updatedAt WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String, updatedAt: String)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY deckId ASC, position ASC, id ASC")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY deckId ASC, position ASC, id ASC")
    suspend fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY position ASC, id ASC")
    suspend fun getForDeck(deckId: String): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(card: CardEntity)

    @Update
    suspend fun update(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteById(id: String)
}
