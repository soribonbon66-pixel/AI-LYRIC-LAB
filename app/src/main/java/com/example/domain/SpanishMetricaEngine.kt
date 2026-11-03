package com.example.domain

import com.example.model.ArtisticFreedomMode
import com.example.model.MetricStatus
import com.example.model.StressType
import com.example.model.SyllablePronunciationOption

data class WordProsody(
    val originalWord: String,
    val cleanWord: String,
    val syllables: List<String>,
    val tonicSyllableIndex: Int,
    val stressType: StressType,
    val startsWithVowelSound: Boolean,
    val endsWithVowelSound: Boolean,
    val hasInternalHiatus: Boolean
)

data class VerseMetricaResult(
    val grammaticalSyllables: Int,
    val poeticSyllables: Int,
    val synalephaCount: Int,
    val synalephaPairs: List<String>,
    val finalWord: String,
    val finalStressType: StressType,
    val wordProsodies: List<WordProsody>,
    val syllableTokens: List<String>,
    val pronunciationOptions: List<SyllablePronunciationOption>
)

object SpanishMetricaEngine {

    private val openVowels = setOf('a', 'e', 'o', 'á', 'é', 'ó')
    private val closedUnstressedVowels = setOf('i', 'u', 'ü')
    private val closedStressedVowels = setOf('í', 'ú')
    private val accentedVowels = setOf('á', 'é', 'í', 'ó', 'ú')
    private val allVowels = openVowels + closedUnstressedVowels + closedStressedVowels

    private val inseparableClusters = setOf(
        "bl", "br", "cl", "cr", "dr", "fl", "fr", "gl", "gr", "pl", "pr", "tr",
        "ch", "ll", "rr"
    )

    fun analyzeVerse(
        line: String,
        customSyllableOverride: Int? = null
    ): VerseMetricaResult {
        val rawWords = tokenizeWords(line)
        if (rawWords.isEmpty()) {
            return VerseMetricaResult(
                grammaticalSyllables = 0,
                poeticSyllables = 0,
                synalephaCount = 0,
                synalephaPairs = emptyList(),
                finalWord = "",
                finalStressType = StressType.LLANA,
                wordProsodies = emptyList(),
                syllableTokens = emptyList(),
                pronunciationOptions = emptyList()
            )
        }

        val prosodies = rawWords.map { analyzeWord(it) }
        val grammaticalCount = prosodies.sumOf { it.syllables.size }

        // Detect poetic synalephas across word boundaries
        val synalephaPairs = mutableListOf<String>()
        var synalephaCount = 0
        var i = 0
        while (i < prosodies.size - 1) {
            val current = prosodies[i]
            val next = prosodies[i + 1]
            if (current.endsWithVowelSound && next.startsWithVowelSound) {
                synalephaCount++
                val lastSyl = current.syllables.lastOrNull() ?: current.cleanWord
                val firstSyl = next.syllables.firstOrNull() ?: next.cleanWord
                synalephaPairs.add("${current.originalWord}‿${next.originalWord} ($lastSyl+$firstSyl)")

                // Check triple vocalic link when middle word is a single vowel/h+vowel (e.g. "va a hacer", "lo he amado")
                if (i + 2 < prosodies.size &&
                    next.syllables.size == 1 &&
                    next.cleanWord.all { it in allVowels || it == 'h' || it == 'y' } &&
                    prosodies[i + 2].startsWithVowelSound
                ) {
                    synalephaCount++
                    synalephaPairs.add("${next.originalWord}‿${prosodies[i + 2].originalWord}")
                    i += 2
                    continue
                }
            }
            i++
        }

        val lastWordProsody = prosodies.last()
        val finalStress = lastWordProsody.stressType
        val stressAdjustment = finalStress.metricAdjustment

        val standardPoetic = (grammaticalCount - synalephaCount + stressAdjustment).coerceAtLeast(1)
        val internalHiatusCount = prosodies.count { it.hasInternalHiatus }

        val options = buildList {
            add(
                SyllablePronunciationOption(
                    id = "standard",
                    label = "Métrica Cantada ($standardPoetic síl.)",
                    syllableCount = standardPoetic,
                    breakdownPreview = buildPoeticPreview(prosodies, applySynalepha = true),
                    explanation = "Aplica $synalephaCount sinalefa(s) y ley del acento final (${finalStress.label})."
                )
            )
            if (synalephaCount > 0) {
                val hiatusPoetic = (grammaticalCount + stressAdjustment).coerceAtLeast(1)
                add(
                    SyllablePronunciationOption(
                        id = "hiatus",
                        label = "Pronunciación Separada / Hiato ($hiatusPoetic síl.)",
                        syllableCount = hiatusPoetic,
                        breakdownPreview = buildPoeticPreview(prosodies, applySynalepha = false),
                        explanation = "Rompe las sinalefas para cantar con pausas marcadas o fraseo staccato."
                    )
                )
            }
            if (internalHiatusCount > 0) {
                val syneresisPoetic = (standardPoetic - internalHiatusCount).coerceAtLeast(1)
                add(
                    SyllablePronunciationOption(
                        id = "syneresis",
                        label = "Sinéresis / Contracción Urbana ($syneresisPoetic síl.)",
                        syllableCount = syneresisPoetic,
                        breakdownPreview = buildPoeticPreview(prosodies, applySynalepha = true) + " [sinéresis]",
                        explanation = "Contrae hiatos internos en una sola emisión vocal rápida."
                    )
                )
            }
            if (grammaticalCount != standardPoetic) {
                add(
                    SyllablePronunciationOption(
                        id = "grammatical",
                        label = "Sílabas Gramaticales ($grammaticalCount síl.)",
                        syllableCount = grammaticalCount,
                        breakdownPreview = prosodies.flatMap { it.syllables }.joinToString("-"),
                        explanation = "Conteo ortográfico puro sin sinalefas ni ajuste por acento final."
                    )
                )
            }
        }

        val effectivePoetic = customSyllableOverride ?: standardPoetic

        return VerseMetricaResult(
            grammaticalSyllables = grammaticalCount,
            poeticSyllables = effectivePoetic,
            synalephaCount = synalephaCount,
            synalephaPairs = synalephaPairs,
            finalWord = lastWordProsody.originalWord,
            finalStressType = finalStress,
            wordProsodies = prosodies,
            syllableTokens = prosodies.flatMap { it.syllables },
            pronunciationOptions = options
        )
    }

    fun evaluateMetricStatus(
        poeticSyllables: Int,
        targetSyllables: Int,
        freedomMode: ArtisticFreedomMode = ArtisticFreedomMode.METRICA_ESTRICTA
    ): MetricStatus {
        if (poeticSyllables == 0) return MetricStatus.NORMAL
        val tolerance = freedomMode.toleranceDelta
        val diff = poeticSyllables - targetSyllables

        return when {
            diff < -tolerance -> MetricStatus.BLUE
            diff <= tolerance -> MetricStatus.NORMAL
            diff == tolerance + 1 -> MetricStatus.YELLOW
            diff in (tolerance + 2)..(tolerance + 3) -> MetricStatus.ORANGE
            else -> MetricStatus.RED
        }
    }

    fun tokenizeWords(line: String): List<String> {
        // Strip bracketed stage directions like [Chorus] or [respira] before counting syllables
        val withoutBrackets = line.replace(Regex("\\[.*?]"), " ")
        return Regex("[a-zA-ZáéíóúüñÁÉÍÓÚÜÑ]+")
            .findAll(withoutBrackets)
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun analyzeWord(rawWord: String): WordProsody {
        val clean = rawWord.lowercase().trim()
        if (clean.isEmpty()) {
            return WordProsody(rawWord, "", emptyList(), 0, StressType.LLANA, false, false, false)
        }

        val syllables = syllabifyWord(clean)
        val tonicIndex = detectTonicSyllableIndex(clean, syllables)
        val stressType = classifyStress(syllables.size, tonicIndex)

        val startsWithVowel = startsWithVowelPhoneme(clean)
        val endsWithVowel = endsWithVowelPhoneme(clean)
        val hasHiatus = checkInternalHiatus(clean)

        return WordProsody(
            originalWord = rawWord,
            cleanWord = clean,
            syllables = syllables,
            tonicSyllableIndex = tonicIndex,
            stressType = stressType,
            startsWithVowelSound = startsWithVowel,
            endsWithVowelSound = endsWithVowel,
            hasInternalHiatus = hasHiatus
        )
    }

    fun syllabifyWord(word: String): List<String> {
        val w = word.lowercase().trim()
        if (w.length <= 2) {
            // Check 2-letter hiatus like "oí", "aé", "ía"
            if (w.length == 2 && isVowelChar(w, 0) && isVowelChar(w, 1) && isHiatusPair(w[0], w[1])) {
                return listOf(w[0].toString(), w[1].toString())
            }
            return listOf(w)
        }

        // 1. Identify vocalic nuclei (diphthongs/triphthongs/single vowels), respecting hiatuses
        data class Nucleus(val start: Int, val endInclusive: Int)
        val nuclei = mutableListOf<Nucleus>()

        var i = 0
        while (i < w.length) {
            if (isVowelChar(w, i)) {
                val nStart = i
                var nEnd = i
                var j = i + 1
                while (j < w.length) {
                    // Intervocalic silent 'h' does not break a diphthong unless vowels form hiatus
                    if (w[j] == 'h' && j + 1 < w.length && isVowelChar(w, j + 1)) {
                        val prevVowel = w[nEnd]
                        val nextVowel = w[j + 1]
                        if (!isHiatusPair(prevVowel, nextVowel)) {
                            nEnd = j + 1
                            j += 2
                            continue
                        } else {
                            break
                        }
                    } else if (isVowelChar(w, j)) {
                        val prevVowel = w[nEnd]
                        val nextVowel = w[j]
                        if (!isHiatusPair(prevVowel, nextVowel)) {
                            nEnd = j
                            j++
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
                nuclei.add(Nucleus(nStart, nEnd))
                i = nEnd + 1
            } else {
                i++
            }
        }

        if (nuclei.size <= 1) {
            return listOf(w)
        }

        // 2. Determine syllable split indices between consecutive nuclei
        val splitPoints = mutableListOf<Int>()
        for (k in 0 until nuclei.size - 1) {
            val currentNucleus = nuclei[k]
            val nextNucleus = nuclei[k + 1]
            val cStart = currentNucleus.endInclusive + 1
            val cEndExclusive = nextNucleus.start
            val consonantSpan = w.substring(cStart, cEndExclusive)

            val splitIndex = when (consonantSpan.length) {
                0 -> cStart // Hiatus directly adjacent
                1 -> cStart // Single consonant goes with next vowel (including 'h' in hiatus like a-ho-ra)
                2 -> {
                    if (isInseparableCluster(consonantSpan, w, cEndExclusive)) {
                        cStart
                    } else {
                        cStart + 1
                    }
                }
                3 -> {
                    val lastTwo = consonantSpan.substring(1, 3)
                    if (isInseparableCluster(lastTwo, w, cEndExclusive)) {
                        cStart + 1
                    } else {
                        cStart + 2
                    }
                }
                else -> cStart + 2
            }
            splitPoints.add(splitIndex)
        }

        val result = mutableListOf<String>()
        var lastIdx = 0
        for (sp in splitPoints) {
            result.add(w.substring(lastIdx, sp))
            lastIdx = sp
        }
        result.add(w.substring(lastIdx))
        return result
    }

    private fun isVowelChar(word: String, index: Int): Boolean {
        val c = word[index]
        if (c in allVowels) {
            // Silent 'u' in 'qu' + e/i or 'gu' + e/i is part of consonant digraph, not a vowel
            if (c == 'u' && index > 0 && (word[index - 1] == 'q' || word[index - 1] == 'g')) {
                val next = word.getOrNull(index + 1)
                if (next == 'e' || next == 'i' || next == 'é' || next == 'í') {
                    return false
                }
            }
            return true
        }
        // Final 'y' acts as closed vowel /i/ (e.g. "hoy", "rey", "estoy", "muy")
        if (c == 'y' && index == word.length - 1 && index > 0) {
            return true
        }
        return false
    }

    private fun isHiatusPair(v1: Char, v2: Char): Boolean {
        val c1 = if (v1 == 'y') 'i' else v1
        val c2 = if (v2 == 'y') 'i' else v2
        // 1. Two open/strong vowels always form hiatus (ca-er, po-e-ta, a-ho-ra, le-ón)
        if (c1 in openVowels && c2 in openVowels) return true
        // 2. Stressed closed vowel (í, ú) + open vowel or open vowel + stressed closed vowel (Ma-rí-a, ba-úl, pa-ís, o-ír)
        if (c1 in closedStressedVowels && (c2 in openVowels || c2 in closedUnstressedVowels)) return true
        if (c2 in closedStressedVowels && (c1 in openVowels || c1 in closedUnstressedVowels)) return true
        // 3. Two identical vowels form hiatus (al-co-hol, chi-i-ta)
        if (normalizeBaseVowel(c1) == normalizeBaseVowel(c2)) return true
        return false
    }

    private fun isInseparableCluster(cluster: String, fullWord: String, clusterEndExclusive: Int): Boolean {
        if (cluster in inseparableClusters) return true
        if ((cluster == "qu" || cluster == "gu") && clusterEndExclusive < fullWord.length) {
            val next = fullWord[clusterEndExclusive]
            if (next == 'e' || next == 'i' || next == 'é' || next == 'í') return true
        }
        return false
    }

    fun detectTonicSyllableIndex(cleanWord: String, syllables: List<String>): Int {
        if (syllables.size <= 1) return 0

        // 1. Check explicit accent mark
        val accentedIdx = syllables.indexOfFirst { syl -> syl.any { it in accentedVowels } }
        if (accentedIdx >= 0) return accentedIdx

        // 2. Apply Spanish natural stress rules
        val lastChar = cleanWord.last()
        return if (lastChar in allVowels || lastChar == 'n' || lastChar == 's') {
            // Llana (penultimate syllable)
            (syllables.size - 2).coerceAtLeast(0)
        } else {
            // Aguda (last syllable)
            syllables.size - 1
        }
    }

    private fun classifyStress(syllableCount: Int, tonicIndex: Int): StressType {
        if (syllableCount == 1) {
            // In Spanish metrics, monosyllables at the end of a verse act as AGUDA (+1 syllable)
            return StressType.AGUDA
        }
        val posFromEnd = syllableCount - 1 - tonicIndex
        return when {
            posFromEnd <= 0 -> StressType.AGUDA
            posFromEnd == 1 -> StressType.LLANA
            else -> StressType.ESDRUJULA
        }
    }

    private fun startsWithVowelPhoneme(cleanWord: String): Boolean {
        val stripped = cleanWord.trimStart('h')
        if (stripped.isEmpty()) return false
        val first = stripped.first()
        return first in allVowels || (first == 'y' && stripped.length == 1)
    }

    private fun endsWithVowelPhoneme(cleanWord: String): Boolean {
        if (cleanWord.isEmpty()) return false
        val last = cleanWord.last()
        return last in allVowels || last == 'y'
    }

    private fun checkInternalHiatus(cleanWord: String): Boolean {
        for (i in 0 until cleanWord.length - 1) {
            val c1 = cleanWord[i]
            val c2 = if (cleanWord[i + 1] == 'h' && i + 2 < cleanWord.length) cleanWord[i + 2] else cleanWord[i + 1]
            if (c1 in allVowels && c2 in allVowels && isHiatusPair(c1, c2)) {
                return true
            }
        }
        return false
    }

    private fun buildPoeticPreview(prosodies: List<WordProsody>, applySynalepha: Boolean): String {
        if (prosodies.isEmpty()) return ""
        val chunks = mutableListOf<String>()
        for (p in prosodies) {
            if (p.syllables.isEmpty()) continue
            if (applySynalepha && chunks.isNotEmpty() && p.startsWithVowelSound) {
                val prev = chunks.removeAt(chunks.lastIndex)
                val prevEndsWithVowel = prev.lastOrNull()?.let { it in allVowels || it == 'y' } == true
                if (prevEndsWithVowel) {
                    chunks.add("${prev}‿${p.syllables.first()}")
                    for (idx in 1 until p.syllables.size) {
                        chunks.add(p.syllables[idx])
                    }
                    continue
                } else {
                    chunks.add(prev)
                }
            }
            chunks.addAll(p.syllables)
        }
        return chunks.joinToString(" · ")
    }

    fun normalizeBaseVowel(c: Char): Char = when (c.lowercaseChar()) {
        'á', 'a' -> 'a'
        'é', 'e' -> 'e'
        'í', 'i', 'y' -> 'i'
        'ó', 'o' -> 'o'
        'ú', 'ü', 'u' -> 'u'
        else -> c.lowercaseChar()
    }
}
