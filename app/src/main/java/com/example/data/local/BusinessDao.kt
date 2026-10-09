package com.example.data.local

import androidx.room.*
import com.example.data.model.Business
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM business WHERE id = 1 LIMIT 1")
    fun getBusinessFlow(): Flow<Business?>

    @Query("SELECT * FROM business WHERE id = 1 LIMIT 1")
    suspend fun getBusiness(): Business?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(business: Business)
}
