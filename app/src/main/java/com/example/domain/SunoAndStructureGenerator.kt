package com.example.domain

import com.example.model.LyricLine
import com.example.model.SongConfig
import com.example.model.SongProject
import com.example.model.SongSection
import com.example.model.SunoExportPackage
import kotlin.math.abs

data class VersionComparisonReport(
    val versionAScore: Int,
    val versionBScore: Int,
    val winnerLabel: String,
    val metricComparison: String,
    val rhymeComparison: String,
    val cadenceComparison: String,
    val detailedVerdict: String
)

object SunoAndStructureGenerator {

    /**
     * Parses any plain-text lyric (pasted by the user or edited in Full Lyrics View)
     * into structured SongSections with automatic poetic syllable target detection.
     */
    fun parseRawLyricsTextIntoSections(
        rawText: String,
        defaultTargetSyllables: Int = 11
    ): List<SongSection> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return listOf(
                SongSection(
                    tag = "[Verse 1]",
                    displayTitle = "Verso 1",
                    targetSyllables = defaultTargetSyllables,
                    vocalDirection = "Tight rhythmic vocal delivery, close-mic presence",
                    lines = listOf(LyricLine(text = ""))
                )
            )
        }

        val rawLines = trimmed.lines()
        val sections = mutableListOf<SongSection>()
        var currentHeaderTag: String? = null
        var currentHeaderTitle: String? = null
        val currentLines = mutableListOf<String>()
        var autoSectionCounter = 1

        fun flushCurrentSection() {
            val cleanLines = currentLines.map { it.trim() }.filter { it.isNotEmpty() }
            if (cleanLines.isEmpty() && currentHeaderTag == null) return

            val tag = currentHeaderTag ?: when (autoSectionCounter) {
                1 -> "[Verse 1]"
                2 -> "[Chorus]"
                3 -> "[Verse 2]"
                4 -> "[Bridge]"
                else -> "[Verse $autoSectionCounter]"
            }
            val title = currentHeaderTitle ?: spanishTitleForTag(tag)
            val englishCue = defaultEnglishVocalCueForTag(tag)

            // Calculate median poetic syllables of the pasted stanza so targetSyllables fits the user's draft
            val syllableCounts = cleanLines.map {
                SpanishMetricaEngine.analyzeVerse(it).poeticSyllables
            }.filter { it > 0 }.sorted()

            val detectedTarget = if (syllableCounts.isNotEmpty()) {
                syllableCounts[syllableCounts.size / 2].coerceIn(6, 16)
            } else {
                defaultTargetSyllables
            }

            sections.add(
                SongSection(
                    tag = tag,
                    displayTitle = title,
                    targetSyllables = detectedTarget,
                    minSyllables = (detectedTarget - 1).coerceAtLeast(5),
                    maxSyllables = detectedTarget + 1,
                    recommendedLines = cleanLines.size.coerceAtLeast(4),
                    rhymePattern = "AABB",
                    accentScheme = "Cadencia detectada ($detectedTarget síl.)",
                    vocalDirection = englishCue,
                    lines = if (cleanLines.isEmpty()) listOf(LyricLine(text = ""))
                    else cleanLines.map { LyricLine(text = it) }
                )
            )
            currentLines.clear()
            currentHeaderTag = null
            currentHeaderTitle = null
            autoSectionCounter++
        }

        for (rawLine in rawLines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (currentLines.isNotEmpty()) {
                    flushCurrentSection()
                }
                continue
            }

            val detectedHeader = detectSectionHeader(line)
            if (detectedHeader != null) {
                if (currentLines.isNotEmpty() || currentHeaderTag != null) {
                    flushCurrentSection()
                }
                currentHeaderTag = detectedHeader.first
                currentHeaderTitle = detectedHeader.second
            } else {
                currentLines.add(line)
            }
        }

        if (currentLines.isNotEmpty() || currentHeaderTag != null) {
            flushCurrentSection()
        }

        return sections.ifEmpty {
            listOf(
                SongSection(
                    tag = "[Verse 1]",
                    displayTitle = "Verso 1",
                    targetSyllables = defaultTargetSyllables,
                    vocalDirection = "Lead vocals, clear articulation",
                    lines = listOf(LyricLine(text = ""))
                )
            )
        }
    }

    private fun detectSectionHeader(line: String): Pair<String, String>? {
        val isBracketed = line.startsWith("[") && line.endsWith("]") && line.length <= 42
        val isColonHeader = line.endsWith(":") && line.split(" ").size <= 4
        val clean = line.removePrefix("[").removeSuffix("]").removeSuffix(":").trim().lowercase()

        val knownHeaderKeywords = listOf(
            "intro", "verso", "verse", "estrofa", "pre-chorus", "pre chorus", "pre-coro",
            "precoro", "pre-estribillo", "chorus", "coro", "estribillo", "hook",
            "bridge", "puente", "outro", "cierre", "final chorus"
        )
        val matchesKeyword = knownHeaderKeywords.any { clean == it || clean.startsWith("$it ") || clean.startsWith("$it-") }

        if (!isBracketed && !isColonHeader && !matchesKeyword) return null
        if (!isBracketed && line.split(" ").size > 4) return null

        val normalizedTag = normalizeSunoSectionTag(clean)
        return normalizedTag to spanishTitleForTag(normalizedTag)
    }

    private fun spanishTitleForTag(tag: String): String {
        val t = tag.lowercase()
        return when {
            "intro" in t -> "Intro"
            "pre" in t -> "Pre-Estribillo"
            "final" in t -> "Estribillo Final"
            "chorus" in t -> "Estribillo (Coro)"
            "verse 1" in t -> "Verso 1"
            "verse 2" in t -> "Verso 2"
            "verse" in t -> "Verso"
            "bridge" in t -> "Puente"
            "outro" in t -> "Outro (Cierre)"
            else -> tag.removePrefix("[").removeSuffix("]")
        }
    }

    fun defaultEnglishVocalCueForTag(tag: String): String {
        val t = tag.lowercase()
        return when {
            "intro" in t -> "Atmospheric filtered vocals, sparse instrumentation, building mood"
            "pre" in t -> "Rising melodic tension, subtle backing harmonies, pre-drop build-up"
            "final" in t -> "Maximum energy climax, doubled lead vocals, anthemic backing choir & ad-libs"
            "chorus" in t -> "Full energy hook, upfront doubled lead vocals, catchy backing vocals"
            "bridge" in t -> "Stripped-back beat, intimate emotional vocal delivery, dynamic contrast"
            "outro" in t -> "Fading instrumental, whispered vocal ad-libs, reverb tail"
            "verse 2" in t -> "Dynamic flow switch, rhythmic bounce, subtle ad-lib echoes"
            else -> "Tight rhythmic lead vocal, clear diction, upfront studio mix"
        }
    }

    fun buildSmartStructureForConfig(config: SongConfig): List<SongSection> {
        val g = config.genre.lowercase()
        val baseSyl = config.defaultTargetSyllables.coerceIn(6, 16)

        return when {
            "reggaet" in g || "perreo" in g || "urbano" in g -> listOf(
                SongSection(
                    tag = "[Intro]",
                    displayTitle = "Intro",
                    targetSyllables = 12,
                    minSyllables = 11,
                    maxSyllables = 12,
                    recommendedLines = 2,
                    rhymePattern = "AA",
                    accentScheme = "Acento en 2 y 4 (Dembow)",
                    vocalDirection = "Atmospheric filtered vocals, distant reverb, spoken ad-libs",
                    lines = listOf(
                        LyricLine(text = "La medianoche cae sobre el cristal"),
                        LyricLine(text = "Hoy tu silueta vuelve a dominar")
                    )
                ),
                SongSection(
                    tag = "[Verse 1]",
                    displayTitle = "Verso 1",
                    targetSyllables = 12,
                    minSyllables = 11,
                    maxSyllables = 12,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    accentScheme = "Cadencia sincopada sobre el bombo",
                    vocalDirection = "Tight melodic-rhythmic flow, dry upfront lead vocal, punchy delivery",
                    lines = listOf(
                        LyricLine(text = "Cruzo la puerta con el humo en el motor"),
                        LyricLine(text = "Buscando el rastro que dejaste en el vapor"),
                        LyricLine(text = "Finges que nada nos conecta en el salón"),
                        LyricLine(text = "Pero tu pulso me delata la intención")
                    )
                ),
                SongSection(
                    tag = "[Pre-Chorus]",
                    displayTitle = "Pre-Estribillo",
                    targetSyllables = 12,
                    minSyllables = 11,
                    maxSyllables = 12,
                    recommendedLines = 4,
                    rhymePattern = "ABAB",
                    accentScheme = "Notas largas preparando el drop",
                    vocalDirection = "Rising melodic tension, wide stereo harmonies, beat build-up",
                    lines = listOf(
                        LyricLine(text = "Se para el tiempo cuando miras sin hablar"),
                        LyricLine(text = "Sube el latido por la piel como un volcán"),
                        LyricLine(text = "No queda freno que nos pueda separar"),
                        LyricLine(text = "Cuando las luces de la pista se nos van")
                    )
                ),
                SongSection(
                    tag = "[Chorus]",
                    displayTitle = "Estribillo (Coro)",
                    targetSyllables = 10,
                    minSyllables = 9,
                    maxSyllables = 11,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    accentScheme = "Rítmico, simétrico y altamente coreable",
                    vocalDirection = "Full energy chorus, doubled lead vocals, call-and-response backing vocals",
                    lines = listOf(
                        LyricLine(text = "Ven y préndelo en la oscuridad"),
                        LyricLine(text = "Que esta noche no pide piedad"),
                        LyricLine(text = "Cae el hielo dentro del ritual"),
                        LyricLine(text = "Nuestro fuego rompe el temporal")
                    )
                ),
                SongSection(
                    tag = "[Verse 2]",
                    displayTitle = "Verso 2",
                    targetSyllables = 11,
                    minSyllables = 10,
                    maxSyllables = 12,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    accentScheme = "Variación en doble tempo ligero",
                    vocalDirection = "Faster percussive bounce, hype ad-libs in background",
                    lines = listOf(
                        LyricLine(text = "Cambia la pauta mientras sube la presión"),
                        LyricLine(text = "Cada palabra va directa al corazón")
                    )
                ),
                SongSection(
                    tag = "[Bridge]",
                    displayTitle = "Puente",
                    targetSyllables = 10,
                    minSyllables = 9,
                    maxSyllables = 11,
                    recommendedLines = 2,
                    rhymePattern = "AA",
                    accentScheme = "Medio tiempo atmosférico",
                    vocalDirection = "Stripped-back instrumental, intimate close-mic vocal, sub-bass only",
                    lines = listOf(
                        LyricLine(text = "Si todo cae cuando salga el sol"),
                        LyricLine(text = "Que nos encuentren dentro del temblor")
                    )
                ),
                SongSection(
                    tag = "[Final Chorus]",
                    displayTitle = "Estribillo Final",
                    targetSyllables = 10,
                    minSyllables = 9,
                    maxSyllables = 11,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    vocalDirection = "Maximum energy climax, anthemic choir harmonies, wide stereo ad-libs",
                    lines = listOf(
                        LyricLine(text = "Ven y préndelo en la oscuridad"),
                        LyricLine(text = "Que esta noche no pide piedad"),
                        LyricLine(text = "Cae el hielo dentro del ritual"),
                        LyricLine(text = "Nuestro fuego rompe el temporal")
                    )
                )
            )

            "trap" in g || "drill" in g || "rap" in g -> listOf(
                SongSection(
                    tag = "[Intro]",
                    displayTitle = "Intro",
                    targetSyllables = 11,
                    minSyllables = 10,
                    maxSyllables = 12,
                    recommendedLines = 2,
                    rhymePattern = "AA",
                    vocalDirection = "Dark 808 glides, low-pitched spoken intro, stereo ad-libs",
                    lines = listOf(
                        LyricLine(text = "Hielo en la vena y el bloque encendido"),
                        LyricLine(text = "Nadie regala lo que hemos subido")
                    )
                ),
                SongSection(
                    tag = "[Chorus]",
                    displayTitle = "Estribillo (Hook)",
                    targetSyllables = 11,
                    minSyllables = 10,
                    maxSyllables = 12,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    vocalDirection = "Melodic trap autotune hook, doubled vocals, heavy 808 drop",
                    lines = listOf(
                        LyricLine(text = "Subo del barro buscando el imperio"),
                        LyricLine(text = "Cero comedia, lo mío va en serio"),
                        LyricLine(text = "Luces de plata cruzando la esquina"),
                        LyricLine(text = "Pura potencia, pura adrenalina")
                    )
                ),
                SongSection(
                    tag = "[Verse 1]",
                    displayTitle = "Verso 1 (Barras)",
                    targetSyllables = 12,
                    minSyllables = 11,
                    maxSyllables = 13,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    vocalDirection = "Aggressive triplet rap flow, punchy consonants, hype backing shouts",
                    lines = listOf(
                        LyricLine(text = "Cuento los pasos que dimos de madrugada"),
                        LyricLine(text = "Nunca doblamos la espalda por casi nada"),
                        LyricLine(text = "Tengo el instinto afilado en el laberinto"),
                        LyricLine(text = "Sueno distinto aunque pise el mismo precinto")
                    )
                ),
                SongSection(
                    tag = "[Outro]",
                    displayTitle = "Outro",
                    targetSyllables = 10,
                    minSyllables = 9,
                    maxSyllables = 11,
                    recommendedLines = 2,
                    rhymePattern = "AA",
                    vocalDirection = "Half-time beat fade-out, atmospheric autotuned ad-libs",
                    lines = listOf(
                        LyricLine(text = "Sigue sonando en el submundo"),
                        LyricLine(text = "Dueños del ritmo en cada segundo")
                    )
                )
            )

            else -> listOf(
                SongSection(
                    tag = "[Verse 1]",
                    displayTitle = "Verso 1",
                    targetSyllables = baseSyl,
                    minSyllables = baseSyl - 1,
                    maxSyllables = baseSyl + 1,
                    recommendedLines = 4,
                    rhymePattern = "ABAB",
                    vocalDirection = "Expressive lead vocal, warm studio compression, clear articulation",
                    lines = listOf(
                        LyricLine(text = "Bajo la lluvia que limpia el cristal"),
                        LyricLine(text = "Suena un acorde que vuelve a empezar"),
                        LyricLine(text = "Cada segundo parece una señal"),
                        LyricLine(text = "De todo aquello que falta contar")
                    )
                ),
                SongSection(
                    tag = "[Chorus]",
                    displayTitle = "Estribillo",
                    targetSyllables = baseSyl,
                    minSyllables = baseSyl - 1,
                    maxSyllables = baseSyl + 1,
                    recommendedLines = 4,
                    rhymePattern = "AABB",
                    vocalDirection = "Soaring anthemic chorus, wide backing harmonies, full dynamic band",
                    lines = listOf(
                        LyricLine(text = "Gira la noche, se enciende la voz"),
                        LyricLine(text = "Nada detiene lo que hay en los dos"),
                        LyricLine(text = "Rompe el silencio como un huracán"),
                        LyricLine(text = "Las melodías que nunca se van")
                    )
                )
            )
        }
    }

    /**
     * Generates a 100% English-directed Suno V6 package:
     * - Style Prompt up to 1000 characters with genre, BPM, time signature, lead voice, backing vocals,
     *   autotune level, vocal timbre, effects, and custom peculiarities in precise English.
     * - Lyrics with precise English structural & vocal performance tags in brackets.
     */
    fun exportForSunoV6(
        project: SongProject,
        maxStyleChars: Int = 1000
    ): SunoExportPackage {
        val config = project.config
        val sunoOpts = config.sunoOptions
        val safeMaxChars = maxStyleChars.coerceIn(100, 1000)

        val genreEnglish = translateGenreToEnglishDescriptor(config.genre, config.subgenre)
        val moodEnglish = translateMoodToEnglish(config.mood)
        val intensityDescriptor = when {
            config.energyIntensity >= 8 -> "high-energy explosive dynamics, festival-ready punch"
            config.energyIntensity >= 5 -> "mid-tempo driving groove, balanced dynamic contour"
            else -> "intimate nocturnal atmosphere, subtle minimal groove"
        }

        val rawStylePrompt = buildString {
            append("Genre: $genreEnglish. ")
            append("Tempo: ${config.bpm} BPM, ${config.timeSignature} time signature. ")
            append("Mood & Atmosphere: $moodEnglish, $intensityDescriptor. ")
            append("Lead Vocals: ${config.voiceType.englishTag}, ${config.deliveryStyle.englishTag}. ")
            append("Vocal Timbre: ${sunoOpts.vocalTimbre}. ")
            if (!sunoOpts.backingVocals.startsWith("None", ignoreCase = true)) {
                append("Backing Vocals & Choirs: ${sunoOpts.backingVocals}. ")
            } else {
                append("Backing Vocals: dry solo lead vocal without backing choir. ")
            }
            append("Pitch & Autotune: ${sunoOpts.autotuneLevel}. ")
            append("Vocal Chain & Effects: ${sunoOpts.vocalEffects}. ")
            if (sunoOpts.customPeculiarities.isNotBlank()) {
                append("Production & Arrangement Peculiarities: ${sunoOpts.customPeculiarities.trim()}. ")
            }
            append("Language: Sung in Spanish with native prosody and clear consonant articulation. ")
            append("Mix Quality: Suno V6 master quality, wide stereo imaging, deep tight low-end, crisp high-end air.")
        }

        val stylePrompt = if (rawStylePrompt.length > safeMaxChars) {
            rawStylePrompt.take(safeMaxChars).substringBeforeLast(".") + "."
        } else {
            rawStylePrompt
        }

        val lyricsBuilder = StringBuilder()
        val structureSummaryItems = mutableListOf<String>()

        for (section in project.sections) {
            val normalizedTag = normalizeSunoSectionTag(section.tag)
            val tagBaseName = normalizedTag.removePrefix("[").removeSuffix("]")
            val englishVocalCue = translateOrEnsureEnglishCue(section.vocalDirection, normalizedTag, config)

            structureSummaryItems.add("$normalizedTag (${section.lines.count { it.text.isNotBlank() }} lines)")

            // Combine section tag and concise English vocal cue in Suno V6 format
            lyricsBuilder.append("[$tagBaseName]\n")
            lyricsBuilder.append("[$englishVocalCue]\n")

            for (line in section.lines) {
                val cleanSungText = line.text.trim()
                if (cleanSungText.isNotEmpty()) {
                    if (line.performanceNote.isNotBlank()) {
                        lyricsBuilder.append("[${line.performanceNote.trim()}] ")
                    }
                    lyricsBuilder.append(cleanSungText).append("\n")
                }
            }
            lyricsBuilder.append("\n")
        }

        val productionRecs = listOf(
            "Style Prompt (${stylePrompt.length}/$safeMaxChars chars): 100% en inglés técnico optimizado para el motor de audio de Suno V6.",
            "Backing Vocals configurados: ${sunoOpts.backingVocals}.",
            "Afinación / Autotune: ${sunoOpts.autotuneLevel} | Timbre: ${sunoOpts.vocalTimbre}.",
            "Efectos y Peculiaridades: ${sunoOpts.vocalEffects} — ${sunoOpts.customPeculiarities.ifBlank { "Standard mix" }}."
        )

        return SunoExportPackage(
            formattedLyrics = lyricsBuilder.toString().trim(),
            stylePrompt = stylePrompt,
            productionRecommendations = productionRecs,
            structureSummary = structureSummaryItems.joinToString(" → ")
        )
    }

    private fun translateGenreToEnglishDescriptor(genre: String, subgenre: String): String {
        val g = genre.lowercase()
        val base = when {
            "reggaet" in g -> "Modern Reggaeton, classic dembow rhythm, heavy sub-bass"
            "perreo" in g -> "Underground Dark Perreo Reggaeton, heavy club dembow"
            "trap" in g -> "Spanish Latin Trap, booming 808 glides, crisp hi-hat rolls"
            "drill" in g -> "Hard-hitting Spanish Drill, sliding 808 bass, syncopated snare"
            "rumba" in g -> "Rumba Gitana & Spanish Catalan Rumba, percussive flamenco guitar ventilador, palmas, cajón"
            "flamenco" in g -> "Flamenco Urbano fusion, Spanish nylon guitar, palmas, modern 808 sub-bass"
            "hardbass" in g -> "High-energy Hardbass electronic, off-beat donk bass, pounding four-on-the-floor kick"
            "makina" in g -> "90s Spanish Makina electronic rave, fast euphoric synth leads, high-BPM kick"
            "trance" in g -> "Uplifting Melodic Trance, supersaw arpeggios, atmospheric breakdown, driving bassline"
            "house" in g -> "Deep & Tech House groove, four-on-the-floor kick, warm bassline, crisp percussion"
            "rap" in g || "boom" in g -> "Boom Bap & Conscious Spanish Rap, punchy acoustic drum break, deep bass"
            "pop" in g -> "Modern Latin Urban Pop, polished synths, radio-ready groove"
            "rock" in g -> "Spanish Alternative Rock, overdriven electric guitars, live studio drums"
            "punk" in g -> "Fast Punk Rock, distorted power chords, raw driving drums"
            else -> genre
        }
        return if (subgenre.isNotBlank()) "$base ($subgenre)" else base
    }

    private fun translateMoodToEnglish(mood: String): String {
        val m = mood.lowercase()
        val parts = mutableListOf<String>()
        if ("nocturn" in m || "oscur" in m) parts.add("dark nocturnal club vibe")
        if ("magnétic" in m || "sensual" in m) parts.add("magnetic sensual tension")
        if ("calle" in m || "crudo" in m || "underground" in m) parts.add("raw street authenticity")
        if ("irónic" in m || "pícar" in m) parts.add("witty ironic swagger")
        if ("épico" in m || "eufóric" in m) parts.add("euphoric anthemic energy")
        if ("melancólic" in m || "triste" in m || "emocional" in m) parts.add("emotional melancholic depth")
        if ("agresiv" in m || "fuerte" in m) parts.add("aggressive intense attitude")
        if (parts.isEmpty()) parts.add(mood)
        return parts.joinToString(", ")
    }

    private fun translateOrEnsureEnglishCue(
        rawDirection: String,
        normalizedTag: String,
        config: SongConfig
    ): String {
        val d = rawDirection.lowercase()
        val isLikelySpanish = listOf("interpretación", "voz", "melódico", "susurrado", "ritmo", "firme", "coros", "estrofa").any { it in d }
        val baseCue = if (rawDirection.isBlank() || isLikelySpanish) {
            defaultEnglishVocalCueForTag(normalizedTag)
        } else {
            rawDirection.trim().removePrefix("[").removeSuffix("]")
        }

        val isChorus = "chorus" in normalizedTag.lowercase()
        return if (isChorus && !config.sunoOptions.backingVocals.startsWith("None", ignoreCase = true)) {
            "$baseCue, ${config.sunoOptions.backingVocals}"
        } else {
            baseCue
        }
    }

    private fun normalizeSunoSectionTag(rawTag: String): String {
        val t = rawTag.trim().removePrefix("[").removeSuffix("]").lowercase()
        val mapped = when {
            "intro" in t -> "Intro"
            "pre" in t -> "Pre-Chorus"
            "final" in t && ("coro" in t || "estribillo" in t || "chorus" in t) -> "Final Chorus"
            "coro" in t || "estribillo" in t || "chorus" in t || "hook" in t -> "Chorus"
            "verso 1" in t || "estrofa 1" in t || "verse 1" in t -> "Verse 1"
            "verso 2" in t || "estrofa 2" in t || "verse 2" in t -> "Verse 2"
            "verso" in t || "estrofa" in t || "verse" in t -> "Verse"
            "puente" in t || "bridge" in t -> "Bridge"
            "outro" in t || "cierre" in t || "final" in t -> "Outro"
            else -> rawTag.trim().removePrefix("[").removeSuffix("]").ifBlank { "Verse" }
        }
        return "[$mapped]"
    }

    fun compareStanzaVersions(
        versionAText: String,
        versionBText: String,
        targetSyllables: Int,
        config: SongConfig
    ): VersionComparisonReport {
        val linesA = versionAText.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { LyricLine(text = it) }
        val linesB = versionBText.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { LyricLine(text = it) }

        val sectionA = SongSection(targetSyllables = targetSyllables, lines = linesA)
        val sectionB = SongSection(targetSyllables = targetSyllables, lines = linesB)

        val analysisA = CadenceAndQualityAnalyzer.analyzeSection(sectionA, config)
        val analysisB = CadenceAndQualityAnalyzer.analyzeSection(sectionB, config)

        val scoreA = if (analysisA.isEmpty()) 0 else analysisA.map { it.score.totalScore }.average().toInt()
        val scoreB = if (analysisB.isEmpty()) 0 else analysisB.map { it.score.totalScore }.average().toInt()

        val avgDevA = if (analysisA.isEmpty()) 0.0 else analysisA.map { abs(it.metricDeviation) }.average()
        val avgDevB = if (analysisB.isEmpty()) 0.0 else analysisB.map { abs(it.metricDeviation) }.average()

        val strongRhymesA = analysisA.count {
            it.rhymeType == com.example.model.RhymeType.CONSONANTE ||
                it.rhymeType == com.example.model.RhymeType.MULTISILABICA ||
                it.rhymeType == com.example.model.RhymeType.ASONANTE
        }
        val strongRhymesB = analysisB.count {
            it.rhymeType == com.example.model.RhymeType.CONSONANTE ||
                it.rhymeType == com.example.model.RhymeType.MULTISILABICA ||
                it.rhymeType == com.example.model.RhymeType.ASONANTE
        }

        val winner = when {
            scoreA > scoreB + 2 -> "Versión A (+${scoreA - scoreB} pts)"
            scoreB > scoreA + 2 -> "Versión B (+${scoreB - scoreA} pts)"
            else -> "Empate técnico"
        }

        val metricComp = "Desviación media respecto a $targetSyllables síl.: Versión A (${String.format("%.1f", avgDevA)} síl.) vs Versión B (${String.format("%.1f", avgDevB)} síl.)."
        val rhymeComp = "Versos con rima sólida: Versión A ($strongRhymesA/${analysisA.size}) vs Versión B ($strongRhymesB/${analysisB.size})."
        val breathA = analysisA.count { !it.cadence.needsBreathWarning }
        val breathB = analysisB.count { !it.cadence.needsBreathWarning }
        val cadenceComp = "Holgura respiratoria a ${config.bpm} BPM: Versión A ($breathA/${analysisA.size} cómodos) vs Versión B ($breathB/${analysisB.size} cómodos)."

        val verdict = buildString {
            if (scoreB >= scoreA) {
                append("La Versión B ofrece mayor rigor técnico ($scoreB/100 frente a $scoreA/100). ")
            } else {
                append("La Versión A mantiene mayor solidez técnica ($scoreA/100 frente a $scoreB/100). ")
            }
            if (avgDevB < avgDevA) {
                append("Además, la Versión B clava con más exactitud la métrica cantada.")
            } else if (avgDevA < avgDevB) {
                append("La Versión A presenta una cuadratura silábica más estable.")
            }
        }

        return VersionComparisonReport(
            versionAScore = scoreA,
            versionBScore = scoreB,
            winnerLabel = winner,
            metricComparison = metricComp,
            rhymeComparison = rhymeComp,
            cadenceComparison = cadenceComp,
            detailedVerdict = verdict
        )
    }
}
