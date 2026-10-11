package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM parsed_resumes ORDER BY timestamp DESC")
    fun getAllResumes(): Flow<List<ResumeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeEntity): Long

    @Query("DELETE FROM parsed_resumes WHERE id = :id")
    suspend fun deleteResume(id: Int)
}
