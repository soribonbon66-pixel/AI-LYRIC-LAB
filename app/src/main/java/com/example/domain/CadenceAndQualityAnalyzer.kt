package com.example.domain

import com.example.model.CadenceAnalysis
import com.example.model.DeliveryStyle
import com.example.model.LineAnalysis
import com.example.model.LineSuggestion
import com.example.model.LyricLine
import com.example.model.MetricStatus
import com.example.model.RhymeType
import com.example.model.SongConfig
import com.example.model.SongSection
import com.example.model.SuggestionType
import com.example.model.VerseScoreBreakdown
import kotlin.math.abs
import kotlin.math.roundToInt

object CadenceAndQualityAnalyzer {

    private val commonCliches = listOf(
        "rompiste el corazón" to "dejaste el pecho en ruinas",
        "me rompiste el corazón" to "apagaste el motor de golpe",
        "morir de amor" to "perder el norte en tu piel",
        "vuelo sin alas" to "subo sin red al abismo",
        "lágrimas de sangre" to "sal quemando en los párpados",
        "fuego de tu amor" to "la brasa que dejaste encendida",
        "como un loco" to "fuera de órbita",
        "sin ti no soy nada" to "sin tu sombra me falta el eje",
        "tocar el cielo" to "rozar el techo del club",
        "dueño de tu corazón" to "quien tiene el código de tu voz",
        "hasta el amanecer" to "hasta que el sol apague el neón",
        "brillas como estrella" to "cortas la sombra como un láser",
        "amor eterno" to "pacto sin fecha de caducidad",
        "llorar por ti" to "gastar la sal en tu nombre"
    )

    private val fillerTokens = setOf(
        "yeah", "yeh", "ey", "oh", "ah", "uh", "baby", "mami", "papi", "ajá", "eh", "la", "na", "uoh", "prr"
    )

    private val stopWords = setOf(
        "el", "la", "los", "las", "un", "una", "de", "del", "al", "en", "con", "por", "para",
        "que", "y", "o", "ni", "se", "me", "te", "mi", "tu", "su", "es", "no", "si", "ya", "lo", "más", "muy"
    )

    private val genericWeakWords = setOf(
        "cosa", "cosas", "todo", "nada", "siempre", "nunca", "algo", "alguien", "hacer", "decir", "pasar"
    )

    fun analyzeSection(
        section: SongSection,
        config: SongConfig
    ): List<LineAnalysis> {
        val rawTexts = section.lines.map { it.text }
        val rhymeAssignments = SpanishRhymeEngine.assignStanzaRhymeLabels(rawTexts)

        // Count content word frequencies across the stanza to detect repetition
        val wordCounts = mutableMapOf<String, Int>()
        for (text in rawTexts) {
            val words = SpanishMetricaEngine.tokenizeWords(text)
                .map { SpanishRhymeEngine.stripAccents(it.lowercase()) }
                .filter { it.length >= 4 && it !in stopWords }
            for (w in words.distinct()) {
                wordCounts[w] = (wordCounts[w] ?: 0) + 1
            }
        }

        val nonEmptyLinesCount = rawTexts.count { it.isNotBlank() }

        return section.lines.mapIndexed { idx, lyricLine ->
            val text = lyricLine.text.trim()
            val metrica = SpanishMetricaEngine.analyzeVerse(
                line = text,
                customSyllableOverride = lyricLine.customPoeticSyllablesOverride
            )
            val target = section.targetSyllables.takeIf { it > 0 } ?: config.defaultTargetSyllables
            val metricStatus = SpanishMetricaEngine.evaluateMetricStatus(
                poeticSyllables = metrica.poeticSyllables,
                targetSyllables = target,
                freedomMode = config.artisticFreedom
            )
            val deviation = if (metrica.poeticSyllables == 0) 0 else metrica.poeticSyllables - target

            val ending = SpanishRhymeEngine.extractPhoneticEnding(metrica.finalWord)
            val (rhymeLetter, rhymeType) = rhymeAssignments.getOrElse(idx) { "-" to RhymeType.LIBRE }
            val internalRhymes = SpanishRhymeEngine.detectInternalRhymes(text)

            val cadence = analyzeCadenceAndBreathing(
                line = text,
                poeticSyllables = metrica.poeticSyllables,
                bpm = config.bpm,
                timeSignature = config.timeSignature,
                deliveryStyle = config.deliveryStyle
            )

            val cliches = detectCliches(text)
            val isFiller = detectFillerLine(text, metrica.poeticSyllables)

            val repeatedInStanza = SpanishMetricaEngine.tokenizeWords(text)
                .filter { w ->
                    val key = SpanishRhymeEngine.stripAccents(w.lowercase())
                    key.length >= 4 && key !in stopWords && (wordCounts[key] ?: 0) >= 2
                }
                .distinct()

            val scoreBreakdown = computeStrictVerseScore(
                text = text,
                poeticSyllables = metrica.poeticSyllables,
                targetSyllables = target,
                rhymeType = rhymeType,
                internalRhymes = internalRhymes,
                cadence = cadence,
                cliches = cliches,
                isFiller = isFiller,
                repeatedWords = repeatedInStanza,
                nonEmptyLinesInStanza = nonEmptyLinesCount,
                config = config
            )

            val quickDiag = buildQuickDiagnostic(
                text = text,
                deviation = deviation,
                metricStatus = metricStatus,
                rhymeType = rhymeType,
                cadence = cadence,
                cliches = cliches,
                isFiller = isFiller,
                score = scoreBreakdown.totalScore
            )

            LineAnalysis(
                lineId = lyricLine.id,
                rawText = lyricLine.text,
                grammaticalSyllables = metrica.grammaticalSyllables,
                poeticSyllables = metrica.poeticSyllables,
                targetSyllables = target,
                metricDeviation = deviation,
                metricStatus = metricStatus,
                synalephaCount = metrica.synalephaCount,
                synalephaPairs = metrica.synalephaPairs,
                finalWord = metrica.finalWord,
                finalStressType = metrica.finalStressType,
                syllableTokens = metrica.syllableTokens,
                consonantRhymeKey = ending.consonantKey,
                assonantRhymeKey = ending.assonantKey,
                vowelSequence = ending.vowelPattern,
                rhymeFamilyLetter = rhymeLetter,
                rhymeType = rhymeType,
                internalRhymes = internalRhymes,
                cadence = cadence,
                detectedCliches = cliches,
                isFillerLine = isFiller,
                repeatedWordsInStanza = repeatedInStanza,
                score = scoreBreakdown,
                pronunciationOptions = metrica.pronunciationOptions,
                quickDiagnostic = quickDiag
            )
        }
    }

    fun analyzeCadenceAndBreathing(
        line: String,
        poeticSyllables: Int,
        bpm: Int,
        timeSignature: String,
        deliveryStyle: DeliveryStyle
    ): CadenceAnalysis {
        val beatsPerBar = when (timeSignature) {
            "3/4" -> 3.0
            "6/8" -> 2.0
            else -> 4.0
        }
        val safeBpm = bpm.coerceIn(40, 220)
        val barDurationSeconds = (60.0 / safeBpm) * beatsPerBar

        if (poeticSyllables == 0) {
            return CadenceAnalysis(
                fitsComfortablyInBar = true,
                estimatedSeconds = 0.0,
                barDurationSeconds = barDurationSeconds,
                syllablesPerSecond = 0.0,
                needsBreathWarning = false,
                suggestedBreathingMarkup = "",
                rhythmAdvice = "Escribe un verso para calcular su encaje en el compás."
            )
        }

        val bpmMultiplier = (safeBpm / 100.0).coerceIn(0.65, 1.75)
        val effectiveSylPerSec = deliveryStyle.syllablesPerSecondFactor * bpmMultiplier
        val estimatedSeconds = poeticSyllables / effectiveSylPerSec
        val barFillRatio = estimatedSeconds / barDurationSeconds

        val needsBreath = poeticSyllables >= 14 || barFillRatio > 1.08
        val fitsComfortably = barFillRatio in 0.50..1.02 && !needsBreath

        val breathingMarkup = if (needsBreath) suggestBreathingPoint(line) else line

        val advice = when {
            needsBreath -> "Exige demasiada toma de aire (${(barFillRatio * 100).roundToInt()}% del compás). Cesura recomendada: $breathingMarkup"
            barFillRatio < 0.45 -> "Frase demasiado breve para el compás; deja un hueco excesivo o exige melisma largo."
            barFillRatio in 0.50..0.90 -> "Encaje óptimo en el compás ($safeBpm BPM, ${deliveryStyle.label})."
            barFillRatio <= 1.02 -> "Cadencia al límite del tiempo fuerte; requiere dicción muy precisa."
            else -> "Desborda el compás; atropellará la entrada del siguiente verso."
        }

        return CadenceAnalysis(
            fitsComfortablyInBar = fitsComfortably,
            estimatedSeconds = (estimatedSeconds * 100).roundToInt() / 100.0,
            barDurationSeconds = (barDurationSeconds * 100).roundToInt() / 100.0,
            syllablesPerSecond = (effectiveSylPerSec * 10).roundToInt() / 10.0,
            needsBreathWarning = needsBreath,
            suggestedBreathingMarkup = breathingMarkup,
            rhythmAdvice = advice
        )
    }

    private fun suggestBreathingPoint(line: String): String {
        val words = line.trim().split(Regex("\\s+"))
        if (words.size < 5) return "$line [breath]"
        val mid = words.size / 2
        val firstHalf = words.subList(0, mid).joinToString(" ")
        val secondHalf = words.subList(mid, words.size).joinToString(" ")
        return "$firstHalf [breath] $secondHalf"
    }

    fun detectCliches(line: String): List<String> {
        val lower = line.lowercase()
        val found = mutableListOf<String>()
        for ((cliche, alternative) in commonCliches) {
            if (cliche in lower) {
                found.add("Cliché gastado: \"$cliche\" → prueba \"$alternative\"")
            }
        }
        return found
    }

    fun detectFillerLine(line: String, poeticSyllables: Int): Boolean {
        val words = SpanishMetricaEngine.tokenizeWords(line).map { it.lowercase() }
        if (words.isEmpty()) return false
        val fillerCount = words.count { it in fillerTokens }
        val contentWords = words.filter { it !in fillerTokens && it !in stopWords }
        return (fillerCount >= 2 && contentWords.size <= 1) || (poeticSyllables in 1..4 && contentWords.size <= 1)
    }

    /**
     * Rigorous, demanding 0-100 scoring system:
     * Mediocre/unrhymed/off-meter lines score 30-55; solid drafts 62-75; studio-ready 78-88; elite 90+.
     */
    private fun computeStrictVerseScore(
        text: String,
        poeticSyllables: Int,
        targetSyllables: Int,
        rhymeType: RhymeType,
        internalRhymes: List<String>,
        cadence: CadenceAnalysis,
        cliches: List<String>,
        isFiller: Boolean,
        repeatedWords: List<String>,
        nonEmptyLinesInStanza: Int,
        config: SongConfig
    ): VerseScoreBreakdown {
        if (text.isBlank()) {
            return VerseScoreBreakdown(0, 0, 0, 0, 0, 0, 0, 0)
        }

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        val words = SpanishMetricaEngine.tokenizeWords(text)
        val contentWords = words.map { SpanishRhymeEngine.stripAccents(it.lowercase()) }
            .filter { it !in stopWords && it !in fillerTokens }
        val genericCount = contentWords.count { it in genericWeakWords }

        // 1. Métrica (0..15) — Strict deviation penalty
        val absDiff = abs(poeticSyllables - targetSyllables)
        val metricScore = when (absDiff) {
            0 -> {
                strengths.add("Métrica exacta clavada en $poeticSyllables sílabas.")
                15
            }
            1 -> {
                improvements.add("Desviación de ${if (poeticSyllables > targetSyllables) "+1" else "-1"} sílaba respecto al objetivo ($targetSyllables).")
                10
            }
            2 -> {
                improvements.add("Desfase de 2 sílabas ($poeticSyllables vs $targetSyllables): obliga a forzar el fraseo.")
                5
            }
            3 -> {
                improvements.add("Desfase severo de 3 sílabas ($poeticSyllables vs $targetSyllables): rompe el patrón rítmico.")
                2
            }
            else -> {
                improvements.add("Fuera de métrica ($poeticSyllables síl. frente a $targetSyllables objetivo).")
                0
            }
        }

        // 2. Calidad de rima (0..15) — Unforgiving on unrhymed, cliché, repeated, or suffix rhymes
        val rhymeScore = when (rhymeType) {
            RhymeType.MULTISILABICA -> {
                strengths.add("Rima multisilábica de alta exigencia técnica.")
                15
            }
            RhymeType.CONSONANTE -> {
                strengths.add("Rima consonante limpia y precisa.")
                13
            }
            RhymeType.ASONANTE -> {
                strengths.add("Rima asonante con buen timbre vocálico.")
                10
            }
            RhymeType.VOCALICA -> 8
            RhymeType.INTERNA, RhymeType.ENCADENADA -> 10
            RhymeType.IMPERFECTA -> 6
            RhymeType.PREVISIBLE -> {
                improvements.add("Rima demasiado previsible/cliché: resta personalidad artística.")
                3
            }
            RhymeType.FORZADA -> {
                improvements.add("Rima fácil con sufijo gramatical (-ando/-iendo/-mente/-ar).")
                2
            }
            RhymeType.REPETIDA -> {
                improvements.add("Fallo de rima: repites exactamente la misma palabra final.")
                0
            }
            RhymeType.LIBRE -> {
                if (nonEmptyLinesInStanza <= 1) {
                    7
                } else if (internalRhymes.isNotEmpty()) {
                    improvements.add("Sin rima final con la estrofa, aunque compensa parcialmente con rima interna.")
                    6
                } else {
                    improvements.add("Verso suelto sin rima consonante ni asonante en la estrofa.")
                    1
                }
            }
        }

        // 3. Musicalidad (0..15)
        var musicality = when {
            !cadence.fitsComfortablyInBar -> 6
            absDiff == 0 -> 11
            absDiff == 1 -> 9
            else -> 5
        }
        if (internalRhymes.isNotEmpty()) {
            musicality = (musicality + 3).coerceAtMost(15)
            strengths.add("Rima interna (${internalRhymes.first()}).")
        }
        if (rhymeType == RhymeType.LIBRE && nonEmptyLinesInStanza > 1) {
            musicality = (musicality - 3).coerceAtLeast(2)
        }

        // 4. Fluidez (0..15)
        var fluency = when {
            cadence.needsBreathWarning -> {
                improvements.add("Demasiado largo para respirar con naturalidad a ${config.bpm} BPM.")
                5
            }
            poeticSyllables < 5 -> {
                improvements.add("Verso excesivamente corto; carece de desarrollo rítmico.")
                5
            }
            absDiff <= 1 -> 12
            else -> 7
        }
        if (words.count { it.length <= 2 } >= 5) {
            fluency = (fluency - 2).coerceAtLeast(3)
            improvements.add("Exceso de monosílabos y partículas que traban la dicción.")
        }

        // 5. Naturalidad lingüística (0..10)
        val naturalness = when {
            rhymeType == RhymeType.FORZADA -> 4
            isFiller -> 3
            absDiff >= 3 -> 5
            else -> 8
        }

        // 6. Originalidad (0..10)
        val originality = when {
            cliches.isNotEmpty() -> {
                improvements.add(cliches.first())
                2
            }
            rhymeType == RhymeType.PREVISIBLE -> 3
            genericCount >= 2 -> {
                improvements.add("Vocabulario genérico (${contentWords.filter { it in genericWeakWords }.joinToString(", ")}); usa imágenes más concretas.")
                4
            }
            contentWords.any { it.length >= 7 } -> 8
            else -> 6
        }

        // 7. Fuerza expresiva (0..10)
        val expressivePower = when {
            isFiller -> {
                improvements.add("Verso de relleno sin aporte semántico ni imagen potente.")
                1
            }
            repeatedWords.isNotEmpty() -> {
                improvements.add("Repetición léxica en la misma estrofa: ${repeatedWords.joinToString(", ")}.")
                4
            }
            contentWords.size < 2 -> {
                improvements.add("Poca densidad de ideas en la línea.")
                3
            }
            genericCount >= 1 -> 5
            else -> 8
        }

        // 8. Coherencia contextual (0..10)
        val coherence = when {
            isFiller -> 2
            contentWords.size < 2 -> 4
            else -> 8
        }

        return VerseScoreBreakdown(
            metricScore = metricScore,
            rhymeScore = rhymeScore,
            musicalityScore = musicality,
            fluencyScore = fluency,
            naturalnessScore = naturalness,
            originalityScore = originality,
            expressivePowerScore = expressivePower,
            contextCoherenceScore = coherence,
            strengths = strengths,
            improvements = improvements
        )
    }

    private fun buildQuickDiagnostic(
        text: String,
        deviation: Int,
        metricStatus: MetricStatus,
        rhymeType: RhymeType,
        cadence: CadenceAnalysis,
        cliches: List<String>,
        isFiller: Boolean,
        score: Int
    ): String {
        if (text.isBlank()) return "Línea vacía"
        return when {
            cliches.isNotEmpty() -> cliches.first()
            isFiller -> "Relleno detectado (${score}/100): sustitúyelo por una imagen o acción concreta."
            metricStatus == MetricStatus.RED -> "Fuera de compás (+$deviation síl.): sobran sílabas para el tempo actual."
            metricStatus == MetricStatus.ORANGE -> "Exceso de +$deviation sílabas: comprime el verso para no atropellar el beat."
            metricStatus == MetricStatus.YELLOW -> "Exceso leve (+1 sílaba): puedes ajustar quitando un artículo o conector."
            metricStatus == MetricStatus.BLUE -> "Verso corto ($deviation síl.): faltan sílabas para alcanzar el patrón."
            rhymeType == RhymeType.LIBRE -> "Métrica correcta, pero el verso está suelto (sin rima con la estrofa)."
            rhymeType == RhymeType.PREVISIBLE -> "Métrica correcta, pero la rima final es cliché y previsible."
            rhymeType == RhymeType.REPETIDA -> "Fallo técnico: repites la misma palabra final para rimar."
            cadence.needsBreathWarning -> "Exige demasiada respiración a este BPM; divide la frase."
            score >= 82 -> "Verso sólido (${score}/100): buen encaje métrico y sonoro."
            else -> "Borrador aceptable (${score}/100), pero puede ganar contundencia y mejor rima."
        }
    }

    fun generateInstantLocalSuggestions(
        line: LyricLine,
        analysis: LineAnalysis
    ): List<LineSuggestion> {
        val text = line.text.trim()
        if (text.isBlank()) return emptyList()
        val suggestions = mutableListOf<LineSuggestion>()

        if (analysis.metricDeviation >= 1) {
            val fillerStripped = text.replace(
                Regex("\\b(ya|pues|entonces|así que|es que|como que|muy|pero|y)\\b\\s*", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            if (fillerStripped != text && fillerStripped.isNotBlank()) {
                val newMetrica = SpanishMetricaEngine.analyzeVerse(fillerStripped)
                suggestions.add(
                    LineSuggestion(
                        type = SuggestionType.REDUCIR_SILABAS,
                        originalText = text,
                        proposedText = fillerStripped.replaceFirstChar { it.uppercase() },
                        reason = "Recorta partículas accesorias para cuadrar con el objetivo de ${analysis.targetSyllables} sílabas.",
                        originalSyllables = analysis.poeticSyllables,
                        proposedSyllables = newMetrica.poeticSyllables,
                        metricDifferenceLabel = "${analysis.poeticSyllables} → ${newMetrica.poeticSyllables} síl.",
                        phoneticImprovement = "Ataque más limpio en el compás."
                    )
                )
            }
        }

        if (analysis.finalWord.isNotBlank()) {
            val betterRhymes = SpanishRhymeEngine.searchRhymes(
                query = analysis.finalWord,
                vocalicModeOnly = false
            ).filter { it.rhymeType != RhymeType.PREVISIBLE }.take(3)

            if (betterRhymes.isNotEmpty() &&
                (analysis.rhymeType == RhymeType.PREVISIBLE || analysis.rhymeType == RhymeType.LIBRE || analysis.rhymeType == RhymeType.REPETIDA)
            ) {
                val altWord = betterRhymes.first().word
                val replaced = text.replaceRange(
                    text.lastIndexOf(analysis.finalWord).coerceAtLeast(0),
                    text.length,
                    altWord
                )
                val newMetrica = SpanishMetricaEngine.analyzeVerse(replaced)
                suggestions.add(
                    LineSuggestion(
                        type = SuggestionType.MEJORAR_RIMA,
                        originalText = text,
                        proposedText = replaced,
                        reason = "Sustituye '${analysis.finalWord}' por '$altWord' (${betterRhymes.first().registerTag}) para evitar rimas débiles.",
                        originalSyllables = analysis.poeticSyllables,
                        proposedSyllables = newMetrica.poeticSyllables,
                        metricDifferenceLabel = "${analysis.poeticSyllables} → ${newMetrica.poeticSyllables} síl.",
                        phoneticImprovement = "Rima ${betterRhymes.first().rhymeType.label} [${betterRhymes.first().vowelPattern}]."
                    )
                )
            }
        }

        if (analysis.cadence.needsBreathWarning) {
            suggestions.add(
                LineSuggestion(
                    type = SuggestionType.MEJORAR_FLUIDEZ,
                    originalText = text,
                    proposedText = analysis.cadence.suggestedBreathingMarkup,
                    reason = "Inserta micro-pausa de aire en mitad del compás.",
                    originalSyllables = analysis.poeticSyllables,
                    proposedSyllables = analysis.poeticSyllables,
                    metricDifferenceLabel = "Cesura respiratoria",
                    phoneticImprovement = "Evita ahogar el final del verso."
                )
            )
        }

        return suggestions
    }
}
