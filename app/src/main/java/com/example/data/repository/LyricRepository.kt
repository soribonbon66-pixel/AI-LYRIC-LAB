package com.example.data.repository

import com.example.data.local.LyricDao
import com.example.data.local.PersonalRhymeEntity
import com.example.data.local.ProjectSerializer
import com.example.data.local.SongProjectEntity
import com.example.data.local.SongVersionEntity
import com.example.domain.SunoAndStructureGenerator
import com.example.model.ArtisticFreedomMode
import com.example.model.DeliveryStyle
import com.example.model.LyricLine
import com.example.model.RhymeType
import com.example.model.SongConfig
import com.example.model.SongMemory
import com.example.model.SongProject
import com.example.model.SongSection
import com.example.model.VoiceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LyricRepository(private val dao: LyricDao) {

    val allProjectsFlow: Flow<List<SongProject>> = dao.getAllProjectsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val personalRhymesFlow: Flow<List<PersonalRhymeEntity>> = dao.getAllPersonalRhymesFlow()

    fun getVersionsFlow(projectId: Long): Flow<List<SongVersionEntity>> =
        dao.getVersionsForProjectFlow(projectId)

    suspend fun ensureInitialSeedData(): SongProject {
        val count = dao.getProjectCount()
        if (count == 0) {
            val defaultConfig = SongConfig(
                genre = "Reggaetón clásico",
                subgenre = "Perreo oscuro de club",
                bpm = 94,
                timeSignature = "4/4",
                mood = "Nocturno, magnético, irónico y callejero",
                language = "Español",
                voiceType = VoiceType.INDIFERENTE,
                deliveryStyle = DeliveryStyle.MELODICA,
                energyIntensity = 8,
                lyricComplexity = "Equilibrada",
                preferredRhymeType = RhymeType.VOCALICA,
                artisticFreedom = ArtisticFreedomMode.METRICA_FLEXIBLE,
                defaultTargetSyllables = 10,
                creativeBlockMode = false
            )

            val defaultMemory = SongMemory(
                centralTheme = "Tensión magnética en el club a las 3 AM entre dos viejos conocidos que fingen indiferencia.",
                storyArc = "Comienza con miradas desde la barra, sube la temperatura en el pre-coro y explota en la pista.",
                characters = "Narrador con actitud tranquila pero directa; contraparte impredecible que domina el club.",
                relationships = "Atracción con orgullo callejero; ninguno quiere ceder primero.",
                emotionalTone = "Magnético, nocturno, con picardía e ironía sutil.",
                culturalReferences = "Neón azul, humo en el VIP, madrugada urbana.",
                wordplayAndIrony = "Contraste entre frío exterior (hielo/cristal) y calor en la pista.",
                characteristicVocab = "pauta, cristal, madrugada, veneno, neón, submundo, voltaje",
                coreMessage = "Aunque disimulen frente a todos, el ritmo los delata cuando cae la luz."
            )

            val starterSections = listOf(
                SongSection(
                    tag = "[Intro]",
                    displayTitle = "Intro (Pauta & Atmósfera)",
                    targetSyllables = 9,
                    minSyllables = 8,
                    maxSyllables = 10,
                    recommendedLines = 2,
                    rhymePattern = "AA",
                    accentScheme = "Entrada suave en 2 y 4",
                    vocalDirection = "Susurrado con reverb larga y filtro telefónico",
                    lines = listOf(
                        LyricLine(text = "La medianoche cae sobre el cristal"),
                        LyricLine(text = "Y tu silueta vuelve a dominar")
                    )
                ),
                SongSection(
                    tag = "[Verse 1]",
                    displayTitle = "Verso 1 (Historia & Cadencia)",
                    targetSyllables = 11,
                    minSyllables = 10,
                    maxSyllables = 12,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    accentScheme = "Dembow marcado con rima interna",
                    vocalDirection = "Melódico firme con ataque percusivo",
                    lines = listOf(
                        LyricLine(text = "Cruzo la puerta con el humo en el motor"),
                        LyricLine(text = "Buscando el rastro que dejaste en el rumor"),
                        LyricLine(text = "Finges que nada nos conecta en el salón"),
                        LyricLine(text = "Pero tu pulso me delata la intención")
                    )
                ),
                SongSection(
                    tag = "[Chorus]",
                    displayTitle = "Estribillo (Gancho Principal)",
                    targetSyllables = 9,
                    minSyllables = 8,
                    maxSyllables = 10,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    accentScheme = "Abierto en vocales A-O y E-A",
                    vocalDirection = "Voz principal doblada con coros de estadio",
                    lines = listOf(
                        LyricLine(text = "Ven y préndelo en la oscuridad"),
                        LyricLine(text = "Que esta noche no pide piedad"),
                        LyricLine(text = "Cae el hielo dentro del ritual"),
                        LyricLine(text = "Nuestro fuego rompe el temporal")
                    )
                )
            )

            val starterProject = SongProject(
                title = "Neón en el Cristal",
                config = defaultConfig,
                memory = defaultMemory,
                sections = starterSections,
                lastScore = 88,
                isFavorite = true
            )

            val id = dao.insertProject(SongProjectEntity.fromDomain(starterProject))

            // Seed initial version snapshot
            dao.insertVersion(
                SongVersionEntity(
                    projectId = id,
                    versionLabel = "Versión inicial de estudio",
                    sectionsJson = ProjectSerializer.sectionsToJson(starterSections),
                    scoreSnapshot = 88
                )
            )

            // Seed second genre project (Trap / Drill)
            val trapConfig = SongConfig(
                genre = "Trap español",
                subgenre = "Fronteo melódico 808",
                bpm = 140,
                timeSignature = "4/4",
                mood = "Ambicioso, crudo, callejero y directo",
                language = "Español",
                voiceType = VoiceType.MASCULINA,
                deliveryStyle = DeliveryStyle.RAPEADA,
                energyIntensity = 9,
                lyricComplexity = "Callejera/Cruda",
                preferredRhymeType = RhymeType.MULTISILABICA,
                artisticFreedom = ArtisticFreedomMode.PRIORIDAD_MUSICALIDAD,
                defaultTargetSyllables = 12
            )
            val trapProject = SongProject(
                title = "Código del Bloque",
                config = trapConfig,
                memory = SongMemory(
                    centralTheme = "Superación desde cero con lealtad al equipo de siempre.",
                    storyArc = "Recuerda las noches sin nada en el estudio casero hasta llenar la sala.",
                    emotionalTone = "Orgulloso, calculador y contundente.",
                    characteristicVocab = "bloque, instinto, laberinto, precinto, plata, submundo",
                    coreMessage = "El trabajo en silencio habla más fuerte que el ruido."
                ),
                sections = SunoAndStructureGenerator.buildSmartStructureForConfig(trapConfig),
                lastScore = 85,
                isFavorite = false
            )
            dao.insertProject(SongProjectEntity.fromDomain(trapProject))

            // Seed Personal Rhyme Bank
            dao.insertPersonalRhyme(
                PersonalRhymeEntity(
                    wordOrPhrase = "instinto / laberinto / distinto / precinto",
                    phoneticFamily = "-into (i-o)",
                    categoryTag = "Multisilábica Trap",
                    notes = "Ideal para barras de 12 sílabas con rima interna en mitad de compás."
                )
            )
            dao.insertPersonalRhyme(
                PersonalRhymeEntity(
                    wordOrPhrase = "oscuridad / electricidad / tempestad / gravedad",
                    phoneticFamily = "-ad (Aguda +1)",
                    categoryTag = "Estribillos Épicos",
                    notes = "Cierra versos agudos sumando +1 sílaba métrica con mucha proyección vocal."
                )
            )
            dao.insertPersonalRhyme(
                PersonalRhymeEntity(
                    wordOrPhrase = "madrugada / emboscada / llamarada / coartada",
                    phoneticFamily = "-ada (a-a)",
                    categoryTag = "Narrativa Nocturna",
                    notes = "Vocales abiertas A-A muy sonoras para melismas en reggaetón y flamenco urbano."
                )
            )

            return starterProject.copy(id = id)
        }

        val first = dao.getAllProjectsFlow()
        val existing = dao.getProjectById(1L)
        return existing?.toDomain() ?: SongProject()
    }

    suspend fun saveProject(project: SongProject, saveVersionLabel: String? = null): SongProject {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val entity = SongProjectEntity.fromDomain(updated)
        val finalId = if (updated.id == 0L) {
            dao.insertProject(entity)
        } else {
            dao.updateProject(entity)
            updated.id
        }

        if (!saveVersionLabel.isNullOrBlank()) {
            dao.insertVersion(
                SongVersionEntity(
                    projectId = finalId,
                    versionLabel = saveVersionLabel,
                    sectionsJson = ProjectSerializer.sectionsToJson(updated.sections),
                    scoreSnapshot = updated.lastScore
                )
            )
        }
        return updated.copy(id = finalId)
    }

    suspend fun createNewProject(
        title: String,
        config: SongConfig,
        memory: SongMemory
    ): SongProject {
        val sections = SunoAndStructureGenerator.buildSmartStructureForConfig(config)
        val project = SongProject(
            title = title.ifBlank { "Proyecto ${config.genre}" },
            config = config,
            memory = memory,
            sections = sections,
            lastScore = 82
        )
        return saveProject(project, saveVersionLabel = "Creación del proyecto")
    }

    suspend fun duplicateProject(project: SongProject): SongProject {
        val copy = project.copy(
            id = 0L,
            title = "${project.title} (Copia)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return saveProject(copy, saveVersionLabel = "Duplicado de '${project.title}'")
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteVersionsForProject(projectId)
        dao.deleteProjectById(projectId)
    }

    suspend fun addPersonalRhyme(
        wordOrPhrase: String,
        phoneticFamily: String,
        categoryTag: String,
        notes: String
    ) {
        dao.insertPersonalRhyme(
            PersonalRhymeEntity(
                wordOrPhrase = wordOrPhrase,
                phoneticFamily = phoneticFamily,
                categoryTag = categoryTag,
                notes = notes
            )
        )
    }

    suspend fun deletePersonalRhyme(id: Long) {
        dao.deletePersonalRhymeById(id)
    }
}
