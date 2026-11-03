package com.example.model

import java.util.UUID

enum class MetricStatus(val label: String, val description: String) {
    BLUE("Azul", "Verso corto (faltan sílabas)"),
    NORMAL("Verde", "Métrica exacta en objetivo"),
    YELLOW("Amarillo", "Exceso leve (+1 sílaba)"),
    ORANGE("Naranja", "Exceso moderado (+2 a +3 sílabas)"),
    RED("Rojo", "Fuera de compás (+4 o más sílabas)")
}

enum class StressType(val label: String, val metricAdjustment: Int) {
    AGUDA("Aguda (+1)", 1),
    LLANA("Llana (0)", 0),
    ESDRUJULA("Esdrújula (-1)", -1)
}

enum class RhymeType(val label: String) {
    CONSONANTE("Consonante"),
    ASONANTE("Asonante"),
    VOCALICA("Vocálica Urbana"),
    MULTISILABICA("Multisilábica"),
    INTERNA("Interna"),
    ENCADENADA("Encadenada"),
    IMPERFECTA("Imperfecta"),
    REPETIDA("Repetida"),
    PREVISIBLE("Previsible / Cliché"),
    FORZADA("Forzada (Sufijo)"),
    LIBRE("Sin rima")
}

enum class VoiceType(val label: String, val englishTag: String) {
    MASCULINA("Masculina", "Male lead vocals"),
    FEMENINA("Femenina", "Female lead vocals"),
    DUO("Dúo", "Male and female vocal duet, trading bars"),
    GRUPO("Grupo / Coro", "Group vocals and gang chants"),
    INDIFERENTE("Indiferente", "Expressive studio lead vocals")
}

enum class DeliveryStyle(val label: String, val syllablesPerSecondFactor: Double, val englishTag: String) {
    MELODICA("Melódica", 2.8, "melodic vocal phrasing, smooth legato"),
    RAPEADA("Rapeada", 4.6, "tight rhythmic rap flow, crisp diction"),
    HABLADA("Hablada / Spoken", 3.6, "intimate spoken-word delivery, close-mic presence"),
    AGRESIVA("Agresiva / Fronteo", 4.2, "aggressive punchy delivery, gritty vocal attitude"),
    SUSURRADA("Susurrada / Íntima", 2.4, "airy whispered vocals, breathy close-up tone"),
    LENTA("Lenta", 2.2, "slow laid-back vocal cadence"),
    NORMAL("Normal", 3.2, "natural studio vocal delivery"),
    RAPIDA("Rápida", 4.8, "fast energetic vocal flow"),
    DOBLE_TEMPO("Doble Tempo", 6.2, "rapid-fire double-time rap cadence"),
    TRILLADA("Trillada / Tresillos", 4.4, "modern triplet-flow bounce")
}

enum class ArtisticFreedomMode(val label: String, val toleranceDelta: Int) {
    METRICA_ESTRICTA("Métrica estricta (±0)", 0),
    METRICA_FLEXIBLE("Métrica flexible (±1)", 1),
    PRIORIDAD_MUSICALIDAD("Prioridad a la musicalidad", 1),
    PRIORIDAD_NATURALIDAD("Prioridad a la naturalidad", 1),
    PRIORIDAD_RIMA("Prioridad a la rima", 1),
    PRIORIDAD_SIGNIFICADO("Prioridad al significado", 2)
}

enum class SuggestionType(val label: String) {
    REDUCIR_SILABAS("Reducir sílabas"),
    ANADIR_SILABAS("Añadir sílabas"),
    CAMBIAR_PALABRA("Cambiar palabra"),
    MEJORAR_RIMA("Mejorar rima"),
    MEJORAR_ACENTUACION("Mejorar acentuación"),
    MEJORAR_FLUIDEZ("Mejorar fluidez"),
    EVITAR_REPETICIONES("Evitar repetición"),
    MEJORAR_NATURALIDAD("Más natural"),
    POTENCIAR_FRASE("Potenciar frase"),
    MEJORAR_MUSICALIDAD("Más musical"),
    MAS_PEGADIZA("Más pegadiza"),
    AUMENTAR_CONTUNDENCIA("Más contundente"),
    MAS_IRONICA("Más irónica"),
    MAS_CALLEJERA("Más callejera"),
    MAS_EMOCIONAL("Más emocional"),
    MAS_DIRECTA("Más directa")
}

enum class RewriteMode(val label: String, val promptInstruction: String) {
    MANTENER_SIGNIFICADO_MEJORAR_METRICA("Cuadrar métrica exacta", "Ajusta exactamente el número de sílabas métricas objetivo conservando el mensaje intacto."),
    MEJORAR_LINEA("Mejorar fluidez y música", "Mejora la musicalidad y fluidez de la línea manteniendo su esencia."),
    CAMBIAR_ESQUEMA_RIMA("Mejorar rima (sin clichés)", "Busca una rima mucho más sonora, original y menos previsible."),
    MAS_PEGADIZA("Hacer más comercial/pegadizo", "Haz el verso más memorable y fácil de corear."),
    MAS_UNDERGROUND("Versión más callejera/cruda", "Usa jerga auténtica, cadencia cruda y actitud real."),
    MAS_AGRESIVA("Versión más contundente", "Incrementa la fuerza expresiva y consonantes percusivas."),
    MAS_NATURAL("Quitar sonido artificial", "Haz que suene 100% natural, como habla y canta una persona real."),
    MAS_ORIGINAL("Romper clichés", "Sustituye imágenes gastadas por un giro fresco e ingenioso."),
    MEJORAR_PALABRA("Cambiar palabra floja", "Sustituye la palabra más débil por otra con mejor encaje fonético."),
    REESCRIBIR_ESTROFA("Reescribir estrofa entera", "Reescribe los versos mejorando su ritmo y cohesión."),
    MANTENER_METRICA_MEJORAR_SIGNIFICADO("Misma métrica + más impacto", "Conserva las sílabas pero haz la imagen mucho más potente."),
    ALTERNATIVAS_ESTRIBILLO("Crear gancho de estribillo", "Genera variaciones memorables y rítmicas para el coro.")
}

data class SunoVocalOptions(
    val backingVocals: String = "Call & Response Ad-libs + Chorus Harmonies",
    val autotuneLevel: String = "Modern Urban Pitch Polish",
    val vocalTimbre: String = "Warm Expressive Chest & Head Voice",
    val vocalEffects: String = "Slapback Delay, Wide Stereo Chorus, Crisp De-Esser",
    val customPeculiarities: String = "Punchy 808 sub-bass, atmospheric intro, dynamic beat drop before chorus"
)

data class SongConfig(
    val genre: String = "Reggaetón clásico",
    val subgenre: String = "Perreo melódico",
    val bpm: Int = 95,
    val timeSignature: String = "4/4",
    val mood: String = "Nocturno, magnético y callejero",
    val language: String = "Español",
    val voiceType: VoiceType = VoiceType.INDIFERENTE,
    val deliveryStyle: DeliveryStyle = DeliveryStyle.MELODICA,
    val energyIntensity: Int = 7,
    val lyricComplexity: String = "Equilibrada",
    val preferredRhymeType: RhymeType = RhymeType.VOCALICA,
    val artisticFreedom: ArtisticFreedomMode = ArtisticFreedomMode.METRICA_ESTRICTA,
    val defaultTargetSyllables: Int = 11,
    val creativeBlockMode: Boolean = false,
    val sunoOptions: SunoVocalOptions = SunoVocalOptions()
)

data class SongMemory(
    val centralTheme: String = "",
    val storyArc: String = "",
    val characters: String = "",
    val relationships: String = "",
    val emotionalTone: String = "",
    val culturalReferences: String = "",
    val wordplayAndIrony: String = "",
    val characteristicVocab: String = "",
    val coreMessage: String = ""
)

data class SyllablePronunciationOption(
    val id: String,
    val label: String,
    val syllableCount: Int,
    val breakdownPreview: String,
    val explanation: String
)

data class LyricLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val customPoeticSyllablesOverride: Int? = null,
    val performanceNote: String = ""
)

data class SongSection(
    val id: String = UUID.randomUUID().toString(),
    val tag: String = "[Verse 1]",
    val displayTitle: String = "Verso 1",
    val targetSyllables: Int = 11,
    val minSyllables: Int = 10,
    val maxSyllables: Int = 12,
    val recommendedLines: Int = 4,
    val rhymePattern: String = "AABB",
    val accentScheme: String = "Fluido en 2 y 4",
    val vocalDirection: String = "Tight rhythmic delivery",
    val lines: List<LyricLine> = emptyList()
)

data class CadenceAnalysis(
    val fitsComfortablyInBar: Boolean,
    val estimatedSeconds: Double,
    val barDurationSeconds: Double,
    val syllablesPerSecond: Double,
    val needsBreathWarning: Boolean,
    val suggestedBreathingMarkup: String,
    val rhythmAdvice: String
)

data class VerseScoreBreakdown(
    val metricScore: Int,           // max 15
    val rhymeScore: Int,            // max 15
    val musicalityScore: Int,       // max 15
    val fluencyScore: Int,          // max 15
    val naturalnessScore: Int,      // max 10
    val originalityScore: Int,      // max 10
    val expressivePowerScore: Int,  // max 10
    val contextCoherenceScore: Int, // max 10
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList()
) {
    val totalScore: Int
        get() = (metricScore + rhymeScore + musicalityScore + fluencyScore +
                naturalnessScore + originalityScore + expressivePowerScore + contextCoherenceScore)
            .coerceIn(0, 100)
}

data class LineAnalysis(
    val lineId: String,
    val rawText: String,
    val grammaticalSyllables: Int,
    val poeticSyllables: Int,
    val targetSyllables: Int,
    val metricDeviation: Int,
    val metricStatus: MetricStatus,
    val synalephaCount: Int,
    val synalephaPairs: List<String>,
    val finalWord: String,
    val finalStressType: StressType,
    val syllableTokens: List<String>,
    val consonantRhymeKey: String,
    val assonantRhymeKey: String,
    val vowelSequence: String,
    val rhymeFamilyLetter: String,
    val rhymeType: RhymeType,
    val internalRhymes: List<String>,
    val cadence: CadenceAnalysis,
    val detectedCliches: List<String>,
    val isFillerLine: Boolean,
    val repeatedWordsInStanza: List<String>,
    val score: VerseScoreBreakdown,
    val pronunciationOptions: List<SyllablePronunciationOption>,
    val quickDiagnostic: String
)

data class LineSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val type: SuggestionType,
    val originalText: String,
    val proposedText: String,
    val reason: String,
    val originalSyllables: Int,
    val proposedSyllables: Int,
    val metricDifferenceLabel: String,
    val phoneticImprovement: String
)

data class RewriteCandidate(
    val text: String,
    val poeticSyllables: Int,
    val rhymeType: String,
    val rationale: String,
    val phoneticNote: String
)

data class GlobalSongEvaluation(
    val overallScore: Int,
    val narrativeCoherence: Int,
    val storyEvolution: Int,
    val stanzaQuality: Int,
    val metricVariety: Int,
    val rhymeVariety: Int,
    val artisticIdentity: Int,
    val originality: Int,
    val absenceOfFiller: Int,
    val commercialPotential: Int,
    val genreFit: Int,
    val executiveSummary: String,
    val highlights: List<String>,
    val actionableFixes: List<String>
)

data class ChorusEvaluation(
    val overallScore: Int,
    val memorability: Int,
    val repeatability: Int,
    val melodicHookPotential: Int,
    val simplicityAndPunch: Int,
    val mainPhraseStrength: Int,
    val vowelSonority: Int,
    val verbalRhythm: Int,
    val naturalness: Int,
    val crowdChantPotential: Int,
    val shortFormViralPotential: Int,
    val detectedWarnings: List<String>,
    val diagnosisSummary: String,
    val catchyAlternatives: List<String>
)

data class RhymeCandidate(
    val word: String,
    val syllables: Int,
    val stressType: StressType,
    val rhymeType: RhymeType,
    val consonantEnding: String,
    val vowelPattern: String,
    val registerTag: String,
    val phoneticAffinity: Int
)

data class SunoExportPackage(
    val formattedLyrics: String,
    val stylePrompt: String,
    val productionRecommendations: List<String>,
    val structureSummary: String
)

data class SongProject(
    val id: Long = 0L,
    val title: String = "Nueva Canción",
    val config: SongConfig = SongConfig(),
    val memory: SongMemory = SongMemory(),
    val sections: List<SongSection> = emptyList(),
    val lastScore: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts the entire song into a single continuous text with section headers
     * so the user can view or edit the full lyrics in one place.
     */
    fun toFullLyricsText(): String {
        return sections.joinToString("\n\n") { sec ->
            buildString {
                append(sec.tag).append("\n")
                append(sec.lines.joinToString("\n") { it.text })
            }
        }
    }
}

object SunoPresets {
    val backingVocalsOptions = listOf(
        "None (Lead Vocal Only)",
        "Subtle Backing Harmonies",
        "Call & Response Ad-libs + Chorus Harmonies",
        "Anthemic Stadium Backing Choir",
        "Female Backing Vocals & Airy Oohs",
        "Male Hype Ad-libs & Doubles",
        "Octave Doubled Lead in Chorus"
    )

    val autotuneOptions = listOf(
        "None (100% Natural Pitch)",
        "Subtle Studio Pitch Correction",
        "Modern Urban Pitch Polish",
        "Heavy Melodic Trap Autotune",
        "Robotic Hard-Tuned Vocal Effect"
    )

    val vocalTimbreOptions = listOf(
        "Warm Expressive Chest & Head Voice",
        "Raspy Emotional Textured Vocal",
        "Dark Baritone Gritty Delivery",
        "Bright Silky Pop-Urban Timbre",
        "Breathless Intimate Close-Mic Whisper",
        "Melismatic Flamenco-Urban Inflection",
        "Aggressive High-Energy Belting"
    )

    val vocalEffectsOptions = listOf(
        "Slapback Delay, Wide Stereo Chorus, Crisp De-Esser",
        "Dry Upfront Studio Vocal + Tight Compression",
        "Telephone Filter Intro + Lush Hall Reverb Chorus",
        "Lush Ambient Shimmer Reverb + Ping-Pong Delay",
        "Tape Saturation + Vintage Tube Warmth"
    )
}

object PredefinedGenres {
    val allGenres = listOf(
        "Reggaetón clásico",
        "Perreo underground",
        "Trap español",
        "Rumba gitana",
        "Hardbass",
        "Makina",
        "Trance",
        "House",
        "Rap / Boom Bap",
        "Drill",
        "Pop urbano",
        "Rock",
        "Punk",
        "Flamenco urbano",
        "Corridos tumbados",
        "R&B contemporáneo",
        "Afrobeat",
        "Bachata urbana"
    )

    fun defaultConfigForGenre(genre: String): Triple<Int, Int, DeliveryStyle> {
        val g = genre.lowercase()
        return when {
            "reggaet" in g || "perreo" in g -> Triple(94, 11, DeliveryStyle.MELODICA)
            "trap" in g -> Triple(140, 11, DeliveryStyle.RAPEADA)
            "drill" in g -> Triple(142, 12, DeliveryStyle.AGRESIVA)
            "rap" in g || "boom" in g -> Triple(90, 12, DeliveryStyle.RAPEADA)
            "rumba" in g || "flamenco" in g -> Triple(105, 8, DeliveryStyle.MELODICA)
            "hardbass" in g -> Triple(155, 8, DeliveryStyle.AGRESIVA)
            "makina" in g -> Triple(170, 8, DeliveryStyle.RAPIDA)
            "trance" in g -> Triple(138, 8, DeliveryStyle.MELODICA)
            "house" in g -> Triple(126, 8, DeliveryStyle.MELODICA)
            "rock" in g -> Triple(120, 10, DeliveryStyle.MELODICA)
            "punk" in g -> Triple(165, 9, DeliveryStyle.AGRESIVA)
            "pop" in g -> Triple(108, 10, DeliveryStyle.MELODICA)
            else -> Triple(100, 10, DeliveryStyle.MELODICA)
        }
    }
}
