package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LyricDatabase
import com.example.data.repository.LyricRepository
import com.example.model.SongConfig
import com.example.model.SongMemory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: LyricDatabase
    private lateinit var repository: LyricRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LyricDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LyricRepository(database.lyricDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app_name and offline draft persistence and version recovery`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Lyric Lab AI", context.getString(R.string.app_name))

        // Seed initial data offline
        val seeded = repository.ensureInitialSeedData()
        assertTrue(seeded.id > 0L)
        assertTrue(seeded.sections.isNotEmpty())

        // Create a new project offline and save a version snapshot
        val customProject = repository.createNewProject(
            title = "Borrador Offline",
            config = SongConfig(genre = "Trap español", bpm = 140),
            memory = SongMemory(centralTheme = "Superación en el estudio")
        )
        val savedWithSnapshot = repository.saveProject(
            customProject.copy(lastScore = 91),
            saveVersionLabel = "Snapshot de prueba offline"
        )

        val allProjects = repository.allProjectsFlow.first()
        assertTrue(allProjects.any { it.title == "Borrador Offline" && it.lastScore == 91 })

        val versions = repository.getVersionsFlow(savedWithSnapshot.id).first()
        assertTrue(versions.any { it.versionLabel == "Snapshot de prueba offline" })
    }
}
