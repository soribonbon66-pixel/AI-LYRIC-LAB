package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricDao {

    @Query("SELECT * FROM song_projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<SongProjectEntity>>

    @Query("SELECT * FROM song_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): SongProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SongProjectEntity): Long

    @Update
    suspend fun updateProject(project: SongProjectEntity)

    @Query("DELETE FROM song_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM song_projects")
    suspend fun getProjectCount(): Int

    // Version history
    @Query("SELECT * FROM song_versions WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getVersionsForProjectFlow(projectId: Long): Flow<List<SongVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: SongVersionEntity): Long

    @Query("DELETE FROM song_versions WHERE projectId = :projectId")
    suspend fun deleteVersionsForProject(projectId: Long)

    // Personal Rhyme Bank
    @Query("SELECT * FROM personal_rhyme_bank ORDER BY createdAt DESC")
    fun getAllPersonalRhymesFlow(): Flow<List<PersonalRhymeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRhyme(entry: PersonalRhymeEntity): Long

    @Query("DELETE FROM personal_rhyme_bank WHERE id = :id")
    suspend fun deletePersonalRhymeById(id: Long)
}
