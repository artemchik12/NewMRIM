package org.artemchik.newmrim.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class MrimDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}