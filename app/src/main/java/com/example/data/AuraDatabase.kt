package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.NoteDao
import com.example.data.dao.SessionDao
import com.example.data.models.Note
import com.example.data.models.Session

@Database(entities = [Session::class, Note::class], version = 2, exportSchema = false)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getDatabase(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
