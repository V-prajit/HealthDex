package com.example.phms.data.local

import androidx.room.*

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodEntity)
}
