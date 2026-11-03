package com.example.data.ai

import android.util.Log
import com.example.data.local.CreativeProfilePreferences
import com.example.domain.CadenceAndQualityAnalyzer
import com.example.domain.SpanishMetricaEngine
import com.example.domain.SpanishRhymeEngine
import com.example.domain.SunoAndStructureGenerator
import com.example.model.ChorusEvaluation
import com.example.model.GlobalSongEvaluation
import com.example.model.LineSuggestion
import com.example.model.LyricLine
import com.example.model.RewriteCandidate
import com.example.model.RewriteMode
import com.example.model.RhymeCandidate
import com.example.model.RhymeType
import com.example.model.SongConfig
import com.example.model.SongMemory
import com.example.model.SongProject
import com.example.model.SongSection
import com.example.model.SuggestionType
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

interface LyricAiService {
    suspend fun generateSmartStructure(
        config: SongConfig,
        memory: SongMemory
    ): List<SongSection>

    suspend fun generateRealTimeSuggestions(
        lineText: String,
        targetSyllables: Int,
        section: SongSection,
        config: SongConfig,
        memory: SongMemory,
        profile: CreativeProfilePreferences
    ): List<LineSuggestion>

    suspend fun rewriteLines(
        originalText: String,
        mode: RewriteMode,
        targetSyllables: Int,
        config: SongConfig,
        memory: SongMemory,
        profile: CreativeProfilePreferences
    ): List<RewriteCandidate>

    suspend fun evaluateFullSong(
        project: SongProject
    ): GlobalSongEvaluation

    suspend fun evaluateAndBoostChorus(
        chorusLines: List<String>,
        config: SongConfig,
        memory: SongMemory
    ): ChorusEvaluation

    suspend fun inferAndUpdateSongMemory(
        project: SongProject
    ): SongMemory

    suspend fun findContextualRhymesAndInspiration(
        queryOrConcept: String,
        vocalicMode: Boolean,
        config: SongConfig,
        memory: SongMemory
    ): Pair<List<RhymeCandidate>, List<String>>
}

class GeminiLyricAiService : LyricAiService {

    companion object {
        private const val TAG = "GeminiLyricAiService"
        // Use high-availability gemini-3.5-flash-lite as primary and gemini-3.8-flash as secondary fallback
        // to avoid "model is currently experiencing high demand" 503 spikes.
        private val MODEL_CHAIN = listOf("gemini-3.5-flash-lite", "gemini-3.8-flash")
        private const val TIMEOUT_MS = 45_000L
    }

    private fun buildModel(modelName: String, schema: Schema): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    /**
     * Executes prompt across the model chain (gemini-3.5-flash-lite -> gemini-3.8-flash)
     * with automatic retry on transient high-demand spikes.
     */
    private suspend fun generateJsonWithFailover(schema: Schema, prompt: String): JSONObject {
        var lastError: Exception? = null
        for (modelName in MODEL_CHAIN) {
            for (attempt in 1..2) {
                try {
                    val model = buildModel(modelName, schema)
                    val response = withTimeout(TIMEOUT_MS) {
                        model.generateContent(prompt)
                    }
                    val rawText = response.text
                    if (!rawText.isNullOrBlank()) {
                        return JSONObject(rawText)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Model $modelName (attempt $attempt) transient error: ${e.message}")
                    if (attempt == 1) {
                        delay(600L)
                    }
                }
            }
        }
        throw lastError ?: IllegalStateException("Servicio IA temporalmente saturado.")
    }

    // Schemas
    private val structureSchema = Schema.obj(
        properties = mapOf(
            "sections" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "tag" to Schema.string("English Suno tag in brackets, e.g. [Intro], [Verse 1], [Pre-Chorus], [Chorus], [Bridge], [Outro]"),
                        "displayTitle" to Schema.string("Spanish descriptive title"),
                        "targetSyllables" to Schema.integer("Target poetic syllables per line"),
                        "minSyllables" to Schema.integer("Minimum recommended syllables"),
                        "maxSyllables" to Schema.integer("Maximum recommended syllables"),
                        "recommendedLines" to Schema.integer("Number of lines in this section"),
                        "rhymePattern" to Schema.string("Rhyme scheme like AABB, ABAB, AAXA"),
                        "accentScheme" to Schema.string("Prosodic accent pattern recommendation"),
                        "vocalDirection" to Schema.string("Vocal delivery and production guidance"),
                        "starterLines" to Schema.array(
                            items = Schema.string(),
                            description = "2 to 4 initial authentic Spanish lyric lines fitting the metric target and mood"
                        )
                    )
                )
            )
        )
    )

    private val suggestionsSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "type" to Schema.string("One of: REDUCIR_SILABAS, ANADIR_SILABAS, CAMBIAR_PALABRA, MEJORAR_RIMA, MEJORAR_ACENTUACION, MEJORAR_FLUIDEZ, EVITAR_REPETICIONES, MEJORAR_NATURALIDAD, POTENCIAR_FRASE, MEJORAR_MUSICALIDAD, MAS_PEGADIZA, AUMENTAR_CONTUNDENCIA, MAS_IRONICA, MAS_CALLEJERA, MAS_EMOCIONAL, MAS_DIRECTA"),
                        "proposedText" to Schema.string("Alternative Spanish lyric line preserving the writer's voice"),
                        "reason" to Schema.string("Concrete technical and artistic reason for this improvement"),
                        "phoneticImprovement" to Schema.string("How vowels, stress, or rhyme improve when sung")
                    )
                )
            )
        )
    )

    private val rewriteSchema = Schema.obj(
        properties = mapOf(
            "candidates" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "text" to Schema.string("Rewritten Spanish line or stanza, natural to sing"),
                        "rhymeType" to Schema.string("Consonante, Asonante, Vocálica, o Multisilábica"),
                        "rationale" to Schema.string("Why this version works rhythmically and semantically"),
                        "phoneticNote" to Schema.string("Accent and vowel cadence note")
                    )
                )
            )
        )
    )

    private val evaluationSchema = Schema.obj(
        properties = mapOf(
            "overallScore" to Schema.integer("Global score 0 to 100"),
            "narrativeCoherence" to Schema.integer("0 to 100"),
            "storyEvolution" to Schema.integer("0 to 100"),
            "stanzaQuality" to Schema.integer("0 to 100"),
            "metricVariety" to Schema.integer("0 to 100"),
            "rhymeVariety" to Schema.integer("0 to 100"),
            "artisticIdentity" to Schema.integer("0 to 100"),
            "originality" to Schema.integer("0 to 100"),
            "absenceOfFiller" to Schema.integer("0 to 100"),
            "commercialPotential" to Schema.integer("0 to 100"),
            "genreFit" to Schema.integer("0 to 100"),
            "executiveSummary" to Schema.string("Detailed producer & songwriter diagnostic in Spanish"),
            "highlights" to Schema.array(items = Schema.string()),
            "actionableFixes" to Schema.array(items = Schema.string())
        )
    )

    private val chorusSchema = Schema.obj(
        properties = mapOf(
            "overallScore" to Schema.integer("0 to 100"),
            "memorability" to Schema.integer("0 to 100"),
            "repeatability" to Schema.integer("0 to 100"),
            "melodicHookPotential" to Schema.integer("0 to 100"),
            "simplicityAndPunch" to Schema.integer("0 to 100"),
            "mainPhraseStrength" to Schema.integer("0 to 100"),
            "vowelSonority" to Schema.integer("0 to 100"),
            "verbalRhythm" to Schema.integer("0 to 100"),
            "naturalness" to Schema.integer("0 to 100"),
            "crowdChantPotential" to Schema.integer("0 to 100"),
            "shortFormViralPotential" to Schema.integer("0 to 100"),
            "detectedWarnings" to Schema.array(items = Schema.string()),
            "diagnosisSummary" to Schema.string(),
            "catchyAlternatives" to Schema.array(items = Schema.string())
        )
    )

    private val memorySchema = Schema.obj(
        properties = mapOf(
            "centralTheme" to Schema.string(),
            "storyArc" to Schema.string(),
            "characters" to Schema.string(),
            "relationships" to Schema.string(),
            "emotionalTone" to Schema.string(),
            "culturalReferences" to Schema.string(),
            "wordplayAndIrony" to Schema.string(),
            "characteristicVocab" to Schema.string(),
            "coreMessage" to Schema.string()
        )
    )

    private val inspirationSchema = Schema.obj(
        properties = mapOf(
            "rhymes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "word" to Schema.string(),
                        "rhymeType" to Schema.string(),
                        "registerTag" to Schema.string()
                    )
                )
            ),
            "creativeIdeas" to Schema.array(items = Schema.string())
        )
    )

    override suspend fun generateSmartStructure(
        config: SongConfig,
        memory: SongMemory
    ): List<SongSection> {
        return try {
            val prompt = """
                Actúa como productor musical y especialista en métrica española.
                Diseña la estructura óptima para:
                - Género: ${config.genre} (${config.subgenre}), ${config.bpm} BPM (${config.timeSignature})
                - Mood: ${config.mood}, Voz: ${config.voiceType.label}, Estilo: ${config.deliveryStyle.label}
                - Tema: ${memory.centralTheme.ifBlank { config.mood }}
                Propón entre 5 y 7 secciones ([Intro], [Verse 1], [Pre-Chorus], [Chorus], [Verse 2], [Bridge], [Final Chorus]) con versos iniciales en español que cuadren exactamente con el número de sílabas poéticas objetivo.
            """.trimIndent()

            val json = generateJsonWithFailover(structureSchema, prompt)
            val arr = json.getJSONArray("sections")
            val result = mutableListOf<SongSection>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val starterArr = obj.optJSONArray("starterLines")
                val lines = mutableListOf<LyricLine>()
                if (starterArr != null) {
                    for (j in 0 until starterArr.length()) {
                        lines.add(LyricLine(text = starterArr.getString(j)))
                    }
                }
                val firstLineSyl = lines.firstOrNull()?.text?.let {
                    SpanishMetricaEngine.analyzeVerse(it).poeticSyllables
                }
                val targetSyl = firstLineSyl?.takeIf { it in 6..16 }
                    ?: obj.optInt("targetSyllables", config.defaultTargetSyllables)

                result.add(
                    SongSection(
                        tag = obj.optString("tag", "[Verse]"),
                        displayTitle = obj.optString("displayTitle", "Sección ${i + 1}"),
                        targetSyllables = targetSyl,
                        minSyllables = (targetSyl - 1).coerceAtLeast(5),
                        maxSyllables = targetSyl + 1,
                        recommendedLines = obj.optInt("recommendedLines", 4),
                        rhymePattern = obj.optString("rhymePattern", "AABB"),
                        accentScheme = obj.optString("accentScheme", "Acentuación rítmica"),
                        vocalDirection = obj.optString("vocalDirection", "Interpretación natural"),
                        lines = lines
                    )
                )
            }
            result.ifEmpty { SunoAndStructureGenerator.buildSmartStructureForConfig(config) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Using local smart structure generator due to cloud spike: ${e.message}")
            SunoAndStructureGenerator.buildSmartStructureForConfig(config)
        }
    }

    override suspend fun generateRealTimeSuggestions(
        lineText: String,
        targetSyllables: Int,
        section: SongSection,
        config: SongConfig,
        memory: SongMemory,
        profile: CreativeProfilePreferences
    ): List<LineSuggestion> {
        val origMetrica = SpanishMetricaEngine.analyzeVerse(lineText)
        return try {
            val contextLines = section.lines.joinToString("\n") { it.text }
            val prompt = """
                Actúa como letrista senior y fonetista español de ${config.genre} (${config.bpm} BPM).
                REGLA FUNDAMENTAL: No destruyas la personalidad, intención ni estilo del autor.
                Contexto: ${memory.centralTheme.ifBlank { config.mood }}
                Estrofa (${section.tag}, objetivo $targetSyllables síl.):
                $contextLines
                Línea actual: "$lineText" (${origMetrica.poeticSyllables} sílabas poéticas; objetivo: $targetSyllables).
                Genera 4 sugerencias naturales de cantar en español (ajustar sílabas a $targetSyllables, mejorar sonoridad vocálica, hacerla más pegadiza y aumentar su fuerza/calle).
            """.trimIndent()

            val json = generateJsonWithFailover(suggestionsSchema, prompt)
            val arr = json.getJSONArray("suggestions")
            val list = mutableListOf<LineSuggestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val proposed = obj.getString("proposedText")
                val propMetrica = SpanishMetricaEngine.analyzeVerse(proposed)
                val rawType = obj.optString("type", "MEJORAR_MUSICALIDAD")
                val typeEnum = runCatching { SuggestionType.valueOf(rawType) }
                    .getOrDefault(SuggestionType.MEJORAR_MUSICALIDAD)
                list.add(
                    LineSuggestion(
                        type = typeEnum,
                        originalText = lineText,
                        proposedText = proposed,
                        reason = obj.optString("reason", "Mejora de cadencia y encaje en compás."),
                        originalSyllables = origMetrica.poeticSyllables,
                        proposedSyllables = propMetrica.poeticSyllables,
                        metricDifferenceLabel = "${origMetrica.poeticSyllables} → ${propMetrica.poeticSyllables} síl. (Obj: $targetSyllables)",
                        phoneticImprovement = obj.optString("phoneticImprovement", "Mayor fluidez vocálica.")
                    )
                )
            }
            list
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to local prosodic suggestion synthesizer: ${e.message}")
            buildLocalSmartSuggestions(lineText, origMetrica.poeticSyllables, targetSyllables, config)
        }
    }

    override suspend fun rewriteLines(
        originalText: String,
        mode: RewriteMode,
        targetSyllables: Int,
        config: SongConfig,
        memory: SongMemory,
        profile: CreativeProfilePreferences
    ): List<RewriteCandidate> {
        return try {
            val prompt = """
                Actúa como compositor profesional de ${config.genre} (${config.bpm} BPM).
                Modo: ${mode.label} -> ${mode.promptInstruction}
                Objetivo métrico: $targetSyllables sílabas poéticas por línea.
                Texto original: "$originalText"
                Genera 5 alternativas naturales que funcionen al cantarse.
            """.trimIndent()

            val json = generateJsonWithFailover(rewriteSchema, prompt)
            val arr = json.getJSONArray("candidates")
            val list = mutableListOf<RewriteCandidate>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val txt = obj.getString("text")
                val firstLine = txt.lines().firstOrNull { it.isNotBlank() } ?: txt
                val metrica = SpanishMetricaEngine.analyzeVerse(firstLine)
                list.add(
                    RewriteCandidate(
                        text = txt,
                        poeticSyllables = metrica.poeticSyllables,
                        rhymeType = obj.optString("rhymeType", "Consonante / Vocálica"),
                        rationale = obj.optString("rationale", ""),
                        phoneticNote = obj.optString("phoneticNote", "")
                    )
                )
            }
            list
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to local rewrite synthesizer: ${e.message}")
            buildLocalRewriteCandidates(originalText, mode, targetSyllables, config)
        }
    }

    override suspend fun evaluateFullSong(
        project: SongProject
    ): GlobalSongEvaluation {
        return try {
            val fullLyrics = project.sections.joinToString("\n\n") { sec ->
                "${sec.tag} (Obj: ${sec.targetSyllables} síl.)\n" +
                    sec.lines.joinToString("\n") { it.text }
            }
            val prompt = """
                Actúa como jurado experto de compositores y productores de ${project.config.genre} (${project.config.bpm} BPM).
                Evalúa esta canción de 0 a 100 adaptando el criterio al género:
                $fullLyrics
            """.trimIndent()

            val json = generateJsonWithFailover(evaluationSchema, prompt)
            val highlightsArr = json.optJSONArray("highlights")
            val fixesArr = json.optJSONArray("actionableFixes")
            GlobalSongEvaluation(
                overallScore = json.optInt("overallScore", 84).coerceIn(0, 100),
                narrativeCoherence = json.optInt("narrativeCoherence", 85).coerceIn(0, 100),
                storyEvolution = json.optInt("storyEvolution", 82).coerceIn(0, 100),
                stanzaQuality = json.optInt("stanzaQuality", 84).coerceIn(0, 100),
                metricVariety = json.optInt("metricVariety", 81).coerceIn(0, 100),
                rhymeVariety = json.optInt("rhymeVariety", 83).coerceIn(0, 100),
                artisticIdentity = json.optInt("artisticIdentity", 87).coerceIn(0, 100),
                originality = json.optInt("originality", 83).coerceIn(0, 100),
                absenceOfFiller = json.optInt("absenceOfFiller", 86).coerceIn(0, 100),
                commercialPotential = json.optInt("commercialPotential", 85).coerceIn(0, 100),
                genreFit = json.optInt("genreFit", 89).coerceIn(0, 100),
                executiveSummary = json.optString("executiveSummary", ""),
                highlights = buildList {
                    if (highlightsArr != null) {
                        for (i in 0 until highlightsArr.length()) add(highlightsArr.getString(i))
                    }
                },
                actionableFixes = buildList {
                    if (fixesArr != null) {
                        for (i in 0 until fixesArr.length()) add(fixesArr.getString(i))
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to local song evaluation engine: ${e.message}")
            buildLocalGlobalEvaluation(project)
        }
    }

    override suspend fun evaluateAndBoostChorus(
        chorusLines: List<String>,
        config: SongConfig,
        memory: SongMemory
    ): ChorusEvaluation {
        return try {
            val chorusText = chorusLines.joinToString("\n")
            val prompt = """
                Actúa como Hitmaker de ${config.genre} (${config.bpm} BPM).
                Evalúa este estribillo de 0 a 100 y genera 5 versiones alternativas ultra pegadizas ("HAZLO MÁS PEGADIZO"):
                $chorusText
            """.trimIndent()

            val json = generateJsonWithFailover(chorusSchema, prompt)
            val warnArr = json.optJSONArray("detectedWarnings")
            val altArr = json.optJSONArray("catchyAlternatives")
            ChorusEvaluation(
                overallScore = json.optInt("overallScore", 86).coerceIn(0, 100),
                memorability = json.optInt("memorability", 86).coerceIn(0, 100),
                repeatability = json.optInt("repeatability", 85).coerceIn(0, 100),
                melodicHookPotential = json.optInt("melodicHookPotential", 87).coerceIn(0, 100),
                simplicityAndPunch = json.optInt("simplicityAndPunch", 84).coerceIn(0, 100),
                mainPhraseStrength = json.optInt("mainPhraseStrength", 86).coerceIn(0, 100),
                vowelSonority = json.optInt("vowelSonority", 88).coerceIn(0, 100),
                verbalRhythm = json.optInt("verbalRhythm", 86).coerceIn(0, 100),
                naturalness = json.optInt("naturalness", 87).coerceIn(0, 100),
                crowdChantPotential = json.optInt("crowdChantPotential", 85).coerceIn(0, 100),
                shortFormViralPotential = json.optInt("shortFormViralPotential", 86).coerceIn(0, 100),
                detectedWarnings = buildList {
                    if (warnArr != null) {
                        for (i in 0 until warnArr.length()) add(warnArr.getString(i))
                    }
                },
                diagnosisSummary = json.optString("diagnosisSummary", ""),
                catchyAlternatives = buildList {
                    if (altArr != null) {
                        for (i in 0 until altArr.length()) add(altArr.getString(i))
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to local chorus hook generator: ${e.message}")
            buildLocalChorusEvaluation(chorusLines, config)
        }
    }

    override suspend fun inferAndUpdateSongMemory(
        project: SongProject
    ): SongMemory {
        return try {
            val allLyrics = project.sections.joinToString("\n\n") { sec ->
                "${sec.tag}\n" + sec.lines.joinToString("\n") { it.text }
            }
            val prompt = """
                Extrae la MEMORIA DE LA CANCIÓN ('${project.title}', ${project.config.genre}) a partir de su letra:
                $allLyrics
            """.trimIndent()

            val json = generateJsonWithFailover(memorySchema, prompt)
            SongMemory(
                centralTheme = json.optString("centralTheme", project.memory.centralTheme),
                storyArc = json.optString("storyArc", project.memory.storyArc),
                characters = json.optString("characters", project.memory.characters),
                relationships = json.optString("relationships", project.memory.relationships),
                emotionalTone = json.optString("emotionalTone", project.memory.emotionalTone),
                culturalReferences = json.optString("culturalReferences", project.memory.culturalReferences),
                wordplayAndIrony = json.optString("wordplayAndIrony", project.memory.wordplayAndIrony),
                characteristicVocab = json.optString("characteristicVocab", project.memory.characteristicVocab),
                coreMessage = json.optString("coreMessage", project.memory.coreMessage)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val words = project.sections.flatMap { it.lines }
                .flatMap { SpanishMetricaEngine.tokenizeWords(it.text) }
                .map { it.lowercase() }
                .filter { it.length >= 5 }
                .distinct()
                .take(10)
            project.memory.copy(
                centralTheme = project.memory.centralTheme.ifBlank { "Atmósfera ${project.config.mood} en estilo ${project.config.genre}" },
                emotionalTone = project.config.mood,
                characteristicVocab = words.joinToString(", "),
                coreMessage = project.memory.coreMessage.ifBlank { "Intensidad y sello propio a ${project.config.bpm} BPM." }
            )
        }
    }

    override suspend fun findContextualRhymesAndInspiration(
        queryOrConcept: String,
        vocalicMode: Boolean,
        config: SongConfig,
        memory: SongMemory
    ): Pair<List<RhymeCandidate>, List<String>> {
        val localRhymes = SpanishRhymeEngine.searchRhymes(queryOrConcept, vocalicModeOnly = vocalicMode)
        return try {
            val prompt = """
                Diccionario fonético e inspiración para ${config.genre} (${config.bpm} BPM, ${config.mood}).
                Búsqueda: "$queryOrConcept" (Modo Vocal: $vocalicMode).
                1. Propón 10 palabras en español que rimen con "$queryOrConcept".
                2. Propón 5 barras o ideas creativas conectadas con "$queryOrConcept".
            """.trimIndent()

            val json = generateJsonWithFailover(inspirationSchema, prompt)
            val rhymesArr = json.optJSONArray("rhymes")
            val ideasArr = json.optJSONArray("creativeIdeas")

            val rhymeCandidates = buildList {
                if (rhymesArr != null) {
                    for (i in 0 until rhymesArr.length()) {
                        val obj = rhymesArr.getJSONObject(i)
                        val w = obj.getString("word")
                        val ending = SpanishRhymeEngine.extractPhoneticEnding(w)
                        add(
                            RhymeCandidate(
                                word = w,
                                syllables = ending.syllableCount,
                                stressType = ending.stressType,
                                rhymeType = if (vocalicMode) RhymeType.VOCALICA else RhymeType.CONSONANTE,
                                consonantEnding = "-${ending.consonantKey}",
                                vowelPattern = ending.vowelPattern,
                                registerTag = obj.optString("registerTag", "Contextual IA"),
                                phoneticAffinity = 95
                            )
                        )
                    }
                }
            }

            val ideas = buildList {
                if (ideasArr != null) {
                    for (i in 0 until ideasArr.length()) add(ideasArr.getString(i))
                }
            }

            (rhymeCandidates + localRhymes).distinctBy { it.word.lowercase() } to ideas
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val fallbackIdeas = listOf(
                "Cruzo la sombra buscando \"$queryOrConcept\" antes de que caiga el telón",
                "En este ${config.genre.lowercase()} a ${config.bpm} BPM nadie apaga la vibración",
                "Guardo el secreto de \"$queryOrConcept\" grabado a fuego en el cristal",
                "Subo el voltaje en la pista hasta romper la gravedad"
            )
            localRhymes to fallbackIdeas
        }
    }

    // --- LOCAL PROSODIC SYNTHESIZER FALLBACKS (Zero Downtime under High Demand) ---

    private fun buildLocalSmartSuggestions(
        lineText: String,
        currentSyllables: Int,
        targetSyllables: Int,
        config: SongConfig
    ): List<LineSuggestion> {
        val words = SpanishMetricaEngine.tokenizeWords(lineText)
        val lastWord = words.lastOrNull() ?: "cristal"
        val rhymes = SpanishRhymeEngine.searchRhymes(lastWord, vocalicModeOnly = false)
            .filter { it.rhymeType != RhymeType.PREVISIBLE }
            .take(4)

        val list = mutableListOf<LineSuggestion>()

        // 1. Metric compression / expansion
        if (currentSyllables > targetSyllables && words.size >= 4) {
            val trimmed = words.drop(1).joinToString(" ").replaceFirstChar { it.uppercase() }
            val m = SpanishMetricaEngine.analyzeVerse(trimmed)
            list.add(
                LineSuggestion(
                    type = SuggestionType.REDUCIR_SILABAS,
                    originalText = lineText,
                    proposedText = trimmed,
                    reason = "Ataque directo sin anacrusa inicial para cuadrar mejor con el objetivo de $targetSyllables sílabas.",
                    originalSyllables = currentSyllables,
                    proposedSyllables = m.poeticSyllables,
                    metricDifferenceLabel = "$currentSyllables → ${m.poeticSyllables} síl. (Obj: $targetSyllables)",
                    phoneticImprovement = "Entrada firme en el tiempo 1 del compás (${config.bpm} BPM)."
                )
            )
        } else if (currentSyllables < targetSyllables) {
            val expanded = "Y ahora ${lineText.replaceFirstChar { it.lowercase() }}"
            val m = SpanishMetricaEngine.analyzeVerse(expanded)
            list.add(
                LineSuggestion(
                    type = SuggestionType.ANADIR_SILABAS,
                    originalText = lineText,
                    proposedText = expanded,
                    reason = "Añade impulso rítmico inicial con sinalefa para acercarse a las $targetSyllables sílabas.",
                    originalSyllables = currentSyllables,
                    proposedSyllables = m.poeticSyllables,
                    metricDifferenceLabel = "$currentSyllables → ${m.poeticSyllables} síl. (Obj: $targetSyllables)",
                    phoneticImprovement = "Apoyo vocálico inicial suave."
                )
            )
        }

        // 2. Phonetic Rhyme & Cadence upgrades
        rhymes.take(3).forEachIndexed { idx, cand ->
            val replaced = if (lineText.contains(lastWord, ignoreCase = true)) {
                lineText.replaceRange(
                    lineText.lastIndexOf(lastWord, ignoreCase = true),
                    lineText.lastIndexOf(lastWord, ignoreCase = true) + lastWord.length,
                    cand.word
                )
            } else {
                "$lineText ${cand.word}"
            }
            val m = SpanishMetricaEngine.analyzeVerse(replaced)
            val sType = when (idx) {
                0 -> SuggestionType.MEJORAR_RIMA
                1 -> SuggestionType.MAS_PEGADIZA
                else -> SuggestionType.AUMENTAR_CONTUNDENCIA
            }
            list.add(
                LineSuggestion(
                    type = sType,
                    originalText = lineText,
                    proposedText = replaced,
                    reason = "Cierra con '${cand.word}' (${cand.registerTag}) para ganar frescura tímbrica en ${config.genre}.",
                    originalSyllables = currentSyllables,
                    proposedSyllables = m.poeticSyllables,
                    metricDifferenceLabel = "$currentSyllables → ${m.poeticSyllables} síl.",
                    phoneticImprovement = "Rima ${cand.rhymeType.label} con secuencia vocálica [${cand.vowelPattern}]."
                )
            )
        }

        return list
    }

    private fun buildLocalRewriteCandidates(
        originalText: String,
        mode: RewriteMode,
        targetSyllables: Int,
        config: SongConfig
    ): List<RewriteCandidate> {
        val baseLine = originalText.lines().firstOrNull { it.isNotBlank() } ?: originalText
        val words = SpanishMetricaEngine.tokenizeWords(baseLine)
        val lastWord = words.lastOrNull() ?: "noche"
        val rhymes = SpanishRhymeEngine.searchRhymes(lastWord, vocalicModeOnly = true).take(5)
        val rWords = rhymes.map { it.word }.ifEmpty {
            listOf("madrugada", "electricidad", "laberinto", "submundo", "adrenalina")
        }

        val variants = listOf(
            "Cae la sombra sobre el borde del ${rWords.getOrElse(0) { "cristal" }}",
            "Sube el pulso cuando entramos al ${rWords.getOrElse(1) { "ritual" }}",
            "Nadie apaga lo que quema en la ${rWords.getOrElse(2) { "ciudad" }}",
            "Gira el rumbo sin pedirnos la ${rWords.getOrElse(3) { "señal" }}",
            "Suena firme cada golpe en el ${rWords.getOrElse(4) { "compás" }}"
        )

        return variants.map { v ->
            val m = SpanishMetricaEngine.analyzeVerse(v)
            RewriteCandidate(
                text = v,
                poeticSyllables = m.poeticSyllables,
                rhymeType = "Consonante / Vocálica",
                rationale = "${mode.label} adaptado a ${config.genre} (${config.bpm} BPM, obj. $targetSyllables síl.).",
                phoneticNote = "Sinalefas fluidas (${m.synalephaCount}‿) y cierre ${m.finalStressType.label}."
            )
        }
    }

    private fun buildLocalGlobalEvaluation(project: SongProject): GlobalSongEvaluation {
        val analyses = project.sections.flatMap { sec ->
            CadenceAndQualityAnalyzer.analyzeSection(sec, project.config)
        }.filter { it.rawText.isNotBlank() }

        if (analyses.isEmpty()) {
            return GlobalSongEvaluation(
                overallScore = 15,
                narrativeCoherence = 20,
                storyEvolution = 15,
                stanzaQuality = 15,
                metricVariety = 20,
                rhymeVariety = 15,
                artisticIdentity = 20,
                originality = 20,
                absenceOfFiller = 30,
                commercialPotential = 15,
                genreFit = 25,
                executiveSummary = "La canción apenas tiene versos escritos. Escribe o pega una letra completa para recibir una valoración técnica real.",
                highlights = emptyList(),
                actionableFixes = listOf("Introduce al menos un Verso y un Estribillo para evaluar métrica, rima y estructura.")
            )
        }

        val rawAvg = analyses.map { it.score.totalScore }.average().toInt()
        val unrhymedRatio = analyses.count { it.rhymeType == RhymeType.LIBRE }.toDouble() / analyses.size
        val severeDeviationRatio = analyses.count { kotlin.math.abs(it.metricDeviation) >= 2 }.toDouble() / analyses.size
        val clichéCount = analyses.count { it.rhymeType == RhymeType.PREVISIBLE || it.rhymeType == RhymeType.FORZADA }
        val shortSongPenalty = if (analyses.size < 6) 14 else if (analyses.size < 10) 6 else 0

        val strictAvg = (rawAvg -
            (unrhymedRatio * 18).toInt() -
            (severeDeviationRatio * 16).toInt() -
            (clichéCount * 4) -
            shortSongPenalty).coerceIn(18, 98)

        val metricScore100 = (analyses.map { it.score.metricScore }.average() / 15.0 * 100.0).toInt().coerceIn(15, 98)
        val rhymeScore100 = (analyses.map { it.score.rhymeScore }.average() / 15.0 * 100.0 - (unrhymedRatio * 20)).toInt().coerceIn(12, 98)
        val origScore100 = (analyses.map { it.score.originalityScore }.average() / 10.0 * 100.0 - (clichéCount * 5)).toInt().coerceIn(15, 98)
        val fillerScore100 = (analyses.map { it.score.naturalnessScore }.average() / 10.0 * 100.0).toInt().coerceIn(20, 98)

        return GlobalSongEvaluation(
            overallScore = strictAvg,
            narrativeCoherence = (strictAvg + 2).coerceIn(15, 96),
            storyEvolution = (strictAvg - shortSongPenalty).coerceIn(15, 95),
            stanzaQuality = ((metricScore100 + rhymeScore100) / 2).coerceIn(15, 96),
            metricVariety = metricScore100,
            rhymeVariety = rhymeScore100,
            artisticIdentity = ((origScore100 + strictAvg) / 2).coerceIn(15, 96),
            originality = origScore100,
            absenceOfFiller = fillerScore100,
            commercialPotential = ((strictAvg + rhymeScore100) / 2).coerceIn(15, 96),
            genreFit = (strictAvg + 4).coerceIn(20, 96),
            executiveSummary = buildString {
                append("Auditoría técnica exigente para '${project.title}' (${project.config.genre} a ${project.config.bpm} BPM): ")
                append("Nota global $strictAvg/100 sobre ${analyses.size} versos activos. ")
                if (severeDeviationRatio > 0.25) {
                    append("Se detecta irregularidad métrica importante (${(severeDeviationRatio * 100).toInt()}% de versos fuera de medida). ")
                }
                if (unrhymedRatio > 0.30) {
                    append("Demasiados versos quedan sueltos sin rima (${(unrhymedRatio * 100).toInt()}%). ")
                }
                if (clichéCount > 0) {
                    append("Hay $clichéCount rimas previsibles o sufijos verbales fáciles que restan impacto.")
                }
            },
            highlights = analyses.filter { it.score.totalScore >= 82 }.take(3)
                .map { "Verso de alto nivel (${it.score.totalScore}/100): \"${it.rawText}\" — métrica y timbre clavados." }
                .ifEmpty { listOf("Ningún verso supera aún el umbral de excelencia (82/100); ajusta las sílabas al objetivo de cada estrofa.") },
            actionableFixes = analyses.sortedBy { it.score.totalScore }.take(4)
                .map { " Línea (${it.score.totalScore}/100) \"${it.rawText}\" → ${it.quickDiagnostic}" }
        )
    }

    private fun buildLocalChorusEvaluation(
        chorusLines: List<String>,
        config: SongConfig
    ): ChorusEvaluation {
        val tempSection = SongSection(
            tag = "[Chorus]",
            displayTitle = "Estribillo",
            targetSyllables = (config.defaultTargetSyllables - 1).coerceAtLeast(7),
            lines = chorusLines.map { LyricLine(text = it) }
        )
        val analyses = CadenceAndQualityAnalyzer.analyzeSection(tempSection, config)
            .filter { it.rawText.isNotBlank() }

        val baseAvg = if (analyses.isEmpty()) 35 else analyses.map { it.score.totalScore }.average().toInt()
        val longLinesCount = analyses.count { it.poeticSyllables > 11 }
        val unrhymedCount = analyses.count { it.rhymeType == RhymeType.LIBRE }
        val strictHookScore = (baseAvg - (longLinesCount * 6) - (unrhymedCount * 7)).coerceIn(20, 96)

        val warnings = mutableListOf<String>()
        if (longLinesCount > 0) {
            warnings.add("Hay $longLinesCount línea(s) de más de 11 sílabas; en un estribillo resta pegada y dificulta que el público lo coree.")
        }
        if (unrhymedCount > 1) {
            warnings.add("Se detectan $unrhymedCount versos sin rima en el estribillo; el gancho pierde recordación inmediata.")
        }
        if (chorusLines.size < 4) {
            warnings.add("Estribillo de solo ${chorusLines.size} línea(s); añade al menos 4 líneas para cerrar una estructura simétrica.")
        }

        return ChorusEvaluation(
            overallScore = strictHookScore,
            memorability = (strictHookScore - longLinesCount * 4).coerceIn(20, 96),
            repeatability = (strictHookScore + 2).coerceIn(20, 96),
            melodicHookPotential = (strictHookScore + 1).coerceIn(20, 96),
            simplicityAndPunch = (strictHookScore - longLinesCount * 5).coerceIn(20, 96),
            mainPhraseStrength = strictHookScore.coerceIn(20, 96),
            vowelSonority = (analyses.map { it.score.musicalityScore }.average() / 15.0 * 100.0).toInt().coerceIn(25, 96),
            verbalRhythm = (analyses.map { it.score.metricScore }.average() / 15.0 * 100.0).toInt().coerceIn(25, 96),
            naturalness = (analyses.map { it.score.fluencyScore }.average() / 15.0 * 100.0).toInt().coerceIn(25, 96),
            crowdChantPotential = (strictHookScore - longLinesCount * 5).coerceIn(20, 96),
            shortFormViralPotential = (strictHookScore - unrhymedCount * 4).coerceIn(20, 96),
            detectedWarnings = warnings,
            diagnosisSummary = "Auditoría exigente de estribillo a ${config.bpm} BPM (${config.genre}): Puntuación de gancho $strictHookScore/100.",
            catchyAlternatives = listOf(
                "Ven y préndelo en la oscuridad\nQue esta noche no pide piedad\nCon el pulso al borde del cristal\nNuestro fuego es pura electricidad",
                "Sube el ritmo cuando cae el sol\nTodo el bloque pierde ya el control\nSin mirar atrás en el espiral\nEsta historia suena sobrenatural",
                "Dime quién apaga este volcán\nCuando las miradas se nos van\nEntre el humo y luces de neón\nManda el pulso sobre la razón",
                "Rompe el hielo dentro del club\nNadie brilla hoy igual que tú\nDe las doce hasta el amanecer\nYa no queda nada que perder",
                "Gira el mundo a nuestra velocidad\nSin candados en la tempestad\nUna chispa basta en el motor\nPara hacer eterno este temblor"
            )
        )
    }
}
