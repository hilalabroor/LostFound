package com.example.lostfound.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/*
 * Database lokal aplikasi.
 *
 * entities:
 * daftar tabel yang digunakan.
 *
 * version:
 * versi struktur database.
 *
 * exportSchema false:
 * untuk project ini kita belum mengekspor
 * riwayat skema database.
 */
@Database(
    entities = [
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

    companion object {

        /*
         * Volatile memastikan seluruh thread membaca
         * instance database yang sama.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context
        ): AppDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val database =
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "lostfound_local_database"
                        ).build()

                    INSTANCE = database

                    database
                }
        }
    }
}