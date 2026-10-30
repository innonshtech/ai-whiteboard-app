package com.avitrisphare.whiteboard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.avitrisphare.whiteboard.data.local.NoteDao
import com.avitrisphare.whiteboard.data.local.FolderDao
import com.avitrisphare.whiteboard.data.local.NoteDatabase
import com.avitrisphare.whiteboard.data.repository.NoteRepositoryImpl
import com.avitrisphare.whiteboard.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNoteDatabase(
        @ApplicationContext context: Context
    ): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            "avitrisphare.db"
        )
            .addMigrations(
                NoteDatabase.MIGRATION_1_2, 
                NoteDatabase.MIGRATION_2_3, 
                NoteDatabase.MIGRATION_3_4,
                NoteDatabase.MIGRATION_4_5,
                NoteDatabase.MIGRATION_5_6
            )
            .fallbackToDestructiveMigration()
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL(
                        "UPDATE notes SET title = 'Welcome to AvitriSphare' " +
                            "WHERE title IN ('Welcome to Avitrisphere', 'Welcome to Avitrisphere Whiteboard')"
                    )
                    db.execSQL(
                        "UPDATE notes SET canvasBackground = 'dotted' WHERE canvasBackground = 'dark'"
                    )
                }

                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val id = java.util.UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO notes (id, title, subtitle, createdAt, updatedAt, strokeDataPath, isFavorite, tags, folder, canvasBackground) " +
                        "VALUES ('$id', 'Welcome to AvitriSphare', '', $now, $now, '', 0, 'tutorial', '', 'dotted')"
                    )
                    
                    // Seed strokes file
                    try {
                        val file = java.io.File(context.filesDir, "strokes_$id.json")
                        val welcomeStrokes = "[{\"points\":\"400.0,400.0;600.0,600.0\",\"colorValue\":-16711936,\"strokeWidth\":8.0,\"isEraser\":false,\"isHighlighter\":false,\"strokeType\":\"CIRCLE\"}]"
                        java.io.FileOutputStream(file).use { it.write(welcomeStrokes.toByteArray()) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideFolderDao(database: NoteDatabase): FolderDao {
        return database.folderDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: NoteRepositoryImpl
    ): NoteRepository
}
