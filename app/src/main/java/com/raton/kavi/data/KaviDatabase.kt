package com.raton.kavi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FolderEntity::class, DeckEntity::class, CardEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KaviDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao

    companion object {
        fun create(context: Context): KaviDatabase = Room.databaseBuilder(
            context.applicationContext,
            KaviDatabase::class.java,
            "kavi.db"
        ).build()
    }
}
