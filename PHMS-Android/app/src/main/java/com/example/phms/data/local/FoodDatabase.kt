package com.example.phms.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FoodEntity::class], version = 1)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile private var INSTANCE: FoodDatabase? = null

        fun get(context: Context): FoodDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FoodDatabase::class.java,
                    "food-cache.db"
                ).build().also { INSTANCE = it }
            }
    }
}
