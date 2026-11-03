package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.SongProject

@Entity(tableName = "song_projects")
data class SongProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val genre: String,
    val bpm: Int,
    val configJson: String,
    val memoryJson: String,
    val sectionsJson: String,
    val lastScore: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SongProject = SongProject(
        id = id,
        title = title,
        config = ProjectSerializer.jsonToConfig(configJson),
        memory = ProjectSerializer.jsonToMemory(memoryJson),
        sections = ProjectSerializer.jsonToSections(sectionsJson),
        lastScore = lastScore,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(project: SongProject): SongProjectEntity = SongProjectEntity(
            id = project.id,
            title = project.title,
            genre = project.config.genre,
            bpm = project.config.bpm,
            configJson = ProjectSerializer.configToJson(project.config),
            memoryJson = ProjectSerializer.memoryToJson(project.memory),
            sectionsJson = ProjectSerializer.sectionsToJson(project.sections),
            lastScore = project.lastScore,
            isFavorite = project.isFavorite,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt
        )
    }
}

@Entity(tableName = "song_versions")
data class SongVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val versionLabel: String,
    val sectionsJson: String,
    val scoreSnapshot: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "personal_rhyme_bank")
data class PersonalRhymeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val wordOrPhrase: String,
    val phoneticFamily: String,
    val categoryTag: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)
