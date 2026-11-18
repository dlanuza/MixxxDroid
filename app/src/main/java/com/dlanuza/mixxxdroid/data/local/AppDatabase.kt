package com.dlanuza.mixxxdroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FolderEntity::class,
        SongEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao

    companion object {

        /**
         * v1 -> v2:
         *  - folders.deckId: lo que ya tenías indexado pasa al Deck A.
         *  - songs: el índice único por uri pasa a ser único por (folderId, uri).
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE folders ADD COLUMN deckId TEXT NOT NULL DEFAULT 'A'"
                )
                db.execSQL("DROP INDEX IF EXISTS index_songs_uri")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_songs_folderId_uri " +
                            "ON songs (folderId, uri)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mixxxdroid.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}