package com.example.domain

import com.example.model.RhymeCandidate
import com.example.model.RhymeType
import com.example.model.StressType

data class PhoneticEnding(
    val originalWord: String,
    val cleanWord: String,
    val consonantKey: String,
    val assonantKey: String,
    val vowelPattern: String, // Last 2-3 syllables vowel sequence for RIMAS POR VOCAL
    val fullVowelChain: String,
    val syllableCount: Int,
    val stressType: StressType,
    val lastTwoSyllablesPhonetic: String
)

object SpanishRhymeEngine {

    private val accentedMap = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u'
    )
    private val baseVowels = setOf('a', 'e', 'i', 'o', 'u')

    // Overused / predictable rhyme pairs in Spanish songwriting
    private val predictablePairs = setOf(
        setOf("corazon", "pasion"),
        setOf("corazon", "razon"),
        setOf("amor", "dolor"),
        setOf("amor", "calor"),
        setOf("cielo", "suelo"),
        setOf("fuego", "juego"),
        setOf("noche", "coche"),
        setOf("vida", "herida"),
        setOf("vida", "salida"),
        setOf("beso", "queso"),
        setOf("dinero", "entero"),
        setOf("llorar", "amar"),
        setOf("mirada", "nada"),
        setOf("locura", "ternura")
    )

    // Grammatical suffixes that feel weak/forced when abused
    private val weakGrammaticalSuffixes = listOf(
        "mente", "ando", "iendo", "aron", "ieron", "aba", "aban"
    )

    private data class LexiconEntry(val word: String, val register: String)

    private val curatedLexicon: List<LexiconEntry> by lazy {
        listOf(
            // -ón / Agudas (o)
            LexiconEntry("corazón", "Emocional"), LexiconEntry("canción", "Musical"),
            LexiconEntry("adicción", "Urbano"), LexiconEntry("tentación", "Nocturno"),
            LexiconEntry("ambición", "Calle"), LexiconEntry("explosión", "Energía"),
            LexiconEntry("ilusión", "Poético"), LexiconEntry("obsesión", "Nocturno"),
            LexiconEntry("dimensión", "Estudio"), LexiconEntry("conexión", "Urbano"),
            LexiconEntry("devoción", "Emocional"), LexiconEntry("maldición", "Oscuro"),
            LexiconEntry("bendición", "Calle"), LexiconEntry("rebelión", "Rock/Punk"),
            LexiconEntry("distorsión", "Electrónica"), LexiconEntry("vibración", "Club"),
            LexiconEntry("escalón", "Calle"), LexiconEntry("callejón", "Calle"),
            LexiconEntry("bofetón", "Agresivo"), LexiconEntry("madrugón", "Nocturno"),
            LexiconEntry("apagón", "Nocturno"), LexiconEntry("huracán", "Energía"),
            LexiconEntry("volcán", "Energía"), LexiconEntry("imán", "Magnético"),
            LexiconEntry("cristal", "Poético"), LexiconEntry("puñal", "Intenso"),
            LexiconEntry("mortal", "Agresivo"), LexiconEntry("señal", "Nocturno"),
            LexiconEntry("ritual", "Nocturno"), LexiconEntry("animal", "Instintivo"),
            LexiconEntry("instinto", "Calle"), LexiconEntry("laberinto", "Poético"),
            LexiconEntry("distinto", "Urbano"), LexiconEntry("precinto", "Calle"),
            // -or / -ar / -er
            LexiconEntry("rumor", "Calle"), LexiconEntry("sudor", "Club"),
            LexiconEntry("motor", "Urbano"), LexiconEntry("temblor", "Intenso"),
            LexiconEntry("vapor", "Nocturno"), LexiconEntry("rencor", "Oscuro"),
            LexiconEntry("fulgor", "Poético"), LexiconEntry("clamor", "Épico"),
            LexiconEntry("error", "Directo"), LexiconEntry("valor", "Calle"),
            LexiconEntry("altar", "Poético"), LexiconEntry("azar", "Destino"),
            LexiconEntry("polar", "Frío"), LexiconEntry("lunar", "Nocturno"),
            LexiconEntry("collar", "Urbano"), LexiconEntry("bulevar", "Nocturno"),
            LexiconEntry("amanecer", "Poético"), LexiconEntry("atardecer", "Poético"),
            LexiconEntry("placer", "Club"), LexiconEntry("poder", "Calle"),
            LexiconEntry("ardor", "Fuego"), LexiconEntry("esplendor", "Poético"),
            // -e-o (fuego, cielo, veneno, deseo...)
            LexiconEntry("deseo", "Nocturno"), LexiconEntry("trofeo", "Calle"),
            LexiconEntry("perreo", "Club"), LexiconEntry("fronteo", "Urbano"),
            LexiconEntry("maleanteo", "Calle"), LexiconEntry("bloqueo", "Directo"),
            LexiconEntry("veneno", "Intenso"), LexiconEntry("trueno", "Energía"),
            LexiconEntry("freno", "Urbano"), LexiconEntry("ajeno", "Emocional"),
            LexiconEntry("sereno", "Poético"), LexiconEntry("terreno", "Calle"),
            LexiconEntry("desvelo", "Nocturno"), LexiconEntry("anhelo", "Poético"),
            LexiconEntry("consuelo", "Emocional"), LexiconEntry("modelo", "Urbano"),
            LexiconEntry("terciopelo", "Sensual"), LexiconEntry("hielo", "Trap"),
            LexiconEntry("acero", "Agresivo"), LexiconEntry("imperio", "Calle"),
            LexiconEntry("misterio", "Nocturno"), LexiconEntry("cementerio", "Oscuro"),
            LexiconEntry("criterio", "Rap"), LexiconEntry("silencio", "Poético"),
            LexiconEntry("espejo", "Poético"), LexiconEntry("reflejo", "Nocturno"),
            LexiconEntry("secreto", "Nocturno"), LexiconEntry("amuleto", "Místico"),
            LexiconEntry("respeto", "Calle"), LexiconEntry("concreto", "Calle"),
            // -a-a (mirada, madrugada, llama, plata...)
            LexiconEntry("madrugada", "Nocturno"), LexiconEntry("emboscada", "Drill"),
            LexiconEntry("llamarada", "Fuego"), LexiconEntry("encrucijada", "Poético"),
            LexiconEntry("coartada", "Calle"), LexiconEntry("estocada", "Rumba"),
            LexiconEntry("fachada", "Calle"), LexiconEntry("oleada", "Energía"),
            LexiconEntry("nostalgia", "Emocional"), LexiconEntry("fragancia", "Sensual"),
            LexiconEntry("distancia", "Emocional"), LexiconEntry("ganancia", "Urbano"),
            LexiconEntry("arrogancia", "Fronteo"), LexiconEntry("elegancia", "Urbano"),
            LexiconEntry("sustancia", "Club"), LexiconEntry("infancia", "Narrativo"),
            LexiconEntry("fantasma", "Oscuro"), LexiconEntry("cicatriz", "Intenso"),
            LexiconEntry("raíz", "Flamenco"), LexiconEntry("matiz", "Poético"),
            LexiconEntry("balanza", "Rap"), LexiconEntry("venganza", "Oscuro"),
            LexiconEntry("esperanza", "Emocional"), LexiconEntry("alianza", "Calle"),
            LexiconEntry("confianza", "Calle"), LexiconEntry("mudanza", "Narrativo"),
            LexiconEntry("plata", "Urbano"), LexiconEntry("escarlata", "Poético"),
            LexiconEntry("pirata", "Calle"), LexiconEntry("serenata", "Rumba"),
            // -i-a (vida, herida, melodía, fantasía, esquina...)
            LexiconEntry("melodía", "Musical"), LexiconEntry("fantasía", "Nocturno"),
            LexiconEntry("agonía", "Intenso"), LexiconEntry("ironía", "Irónico"),
            LexiconEntry("cobardía", "Directo"), LexiconEntry("rebeldía", "Rock/Urbano"),
            LexiconEntry("energía", "Club"), LexiconEntry("euforia", "Trance/House"),
            LexiconEntry("memoria", "Emocional"), LexiconEntry("victoria", "Épico"),
            LexiconEntry("historia", "Narrativo"), LexiconEntry("esquina", "Calle"),
            LexiconEntry("neblina", "Nocturno"), LexiconEntry("adrenalina", "Energía"),
            LexiconEntry("vitrina", "Urbano"), LexiconEntry("ruina", "Oscuro"),
            LexiconEntry("espina", "Flamenco"), LexiconEntry("doctrina", "Rap"),
            LexiconEntry("gasolina", "Club"), LexiconEntry("medicina", "Urbano"),
            LexiconEntry("mentira", "Directo"), LexiconEntry("ceniza", "Poético"),
            LexiconEntry("sonrisa", "Emocional"), LexiconEntry("cornisa", "Tensión"),
            LexiconEntry("caricia", "Sensual"), LexiconEntry("codicia", "Calle"),
            LexiconEntry("malicia", "Urbano"), LexiconEntry("justicia", "Rap"),
            // -i-o (camino, destino, olvido, latido...)
            LexiconEntry("camino", "Narrativo"), LexiconEntry("destino", "Poético"),
            LexiconEntry("vino", "Rumba"), LexiconEntry("torbellino", "Energía"),
            LexiconEntry("asesino", "Drill/Trap"), LexiconEntry("clandestino", "Nocturno"),
            LexiconEntry("diamantino", "Brillo"), LexiconEntry("desatino", "Emocional"),
            LexiconEntry("olvido", "Emocional"), LexiconEntry("latido", "Musical"),
            LexiconEntry("ruido", "Club"), LexiconEntry("prohibido", "Sensual"),
            LexiconEntry("bandido", "Urbano"), LexiconEntry("sentido", "Poético"),
            LexiconEntry("herido", "Emocional"), LexiconEntry("perdido", "Nocturno"),
            LexiconEntry("abismo", "Oscuro"), LexiconEntry("espejismo", "Poético"),
            LexiconEntry("cinismo", "Irónico"), LexiconEntry("magnetismo", "Sensual"),
            LexiconEntry("peligro", "Calle"), LexiconEntry("zafiro", "Brillo"),
            LexiconEntry("suspiro", "Emocional"), LexiconEntry("vampiro", "Nocturno"),
            LexiconEntry("delirio", "Intenso"), LexiconEntry("martirio", "Flamenco"),
            // -u-a / -u-o (luna, fortuna, locura, humo, pulso...)
            LexiconEntry("luna", "Nocturno"), LexiconEntry("fortuna", "Urbano"),
            LexiconEntry("laguna", "Poético"), LexiconEntry("cuna", "Rumba"),
            LexiconEntry("ninguna", "Directo"), LexiconEntry("oportuna", "Rap"),
            LexiconEntry("altura", "Urbano"), LexiconEntry("armadura", "Épico"),
            LexiconEntry("amargura", "Emocional"), LexiconEntry("aventura", "Club"),
            LexiconEntry("fractura", "Intenso"), LexiconEntry("censura", "Punk/Rap"),
            LexiconEntry("velocidad", "Makina"), LexiconEntry("oscuridad", "Nocturno"),
            LexiconEntry("eternidad", "Trance"), LexiconEntry("tempestad", "Energía"),
            LexiconEntry("lealtad", "Calle"), LexiconEntry("libertad", "Rock"),
            LexiconEntry("realidad", "Rap"), LexiconEntry("gravedad", "Espacial"),
            LexiconEntry("humo", "Trap"), LexiconEntry("pulso", "Club"),
            LexiconEntry("impulso", "Energía"), LexiconEntry("rumbo", "Narrativo"),
            LexiconEntry("submundo", "Underground"), LexiconEntry("segundo", "Tiempo"),
            LexiconEntry("profundo", "Poético"), LexiconEntry("vagabundo", "Rumba"),
            // -o-e / -o-a (noche, bronce, sombra, corona...)
            LexiconEntry("bronce", "Flamenco"), LexiconEntry("roce", "Sensual"),
            LexiconEntry("derroche", "Urbano"), LexiconEntry("reproche", "Emocional"),
            LexiconEntry("medianoche", "Nocturno"), LexiconEntry("horizonte", "Poético"),
            LexiconEntry("sombra", "Nocturno"), LexiconEntry("alfombra", "Urbano"),
            LexiconEntry("corona", "Calle"), LexiconEntry("zona", "Urbano"),
            LexiconEntry("leona", "Fuerza"), LexiconEntry("persona", "Directo"),
            LexiconEntry("ola", "Verano"), LexiconEntry("pistola", "Drill"),
            LexiconEntry("consola", "Estudio"), LexiconEntry("aureola", "Poético"),
            LexiconEntry("aurora", "Poético"), LexiconEntry("demora", "Tiempo"),
            LexiconEntry("caoba", "Sensual"), LexiconEntry("alcoba", "Íntimo")
        )
    }

    fun extractPhoneticEnding(rawWord: String): PhoneticEnding {
        val prosody = SpanishMetricaEngine.analyzeWord(rawWord)
        val clean = prosody.cleanWord
        if (clean.isEmpty() || prosody.syllables.isEmpty()) {
            return PhoneticEnding(rawWord, "", "", "", "", "", 0, StressType.LLANA, "")
        }

        val syllables = prosody.syllables
        val tonicIdx = prosody.tonicSyllableIndex.coerceIn(0, syllables.lastIndex)

        // Build substring from the tonic syllable onward
        val fromTonicSyllable = syllables.subList(tonicIdx, syllables.size).joinToString("")
        val normalizedFromTonic = normalizeSpanishPhonetics(fromTonicSyllable)

        // Find first vowel inside normalizedFromTonic (which is the tonic nucleus)
        val firstVowelIdx = normalizedFromTonic.indexOfFirst { it in baseVowels }
        val consonantKey = if (firstVowelIdx >= 0) {
            normalizedFromTonic.substring(firstVowelIdx)
        } else {
            normalizedFromTonic
        }

        val assonantKey = consonantKey.filter { it in baseVowels }.toCharArray().joinToString("-")

        // Extract vowel pattern of last 2-3 syllables for RIMAS POR VOCAL
        val tailSyllables = syllables.takeLast(minOf(3, syllables.size))
        val vowelPattern = tailSyllables.mapNotNull { syl ->
            extractPrincipalVowel(syl)
        }.joinToString("-")

        val fullVowelChain = syllables.mapNotNull { extractPrincipalVowel(it) }.joinToString("-")

        val lastTwoPhonetic = syllables.takeLast(minOf(2, syllables.size))
            .joinToString("") { normalizeSpanishPhonetics(it) }

        return PhoneticEnding(
            originalWord = rawWord,
            cleanWord = clean,
            consonantKey = consonantKey,
            assonantKey = assonantKey,
            vowelPattern = vowelPattern,
            fullVowelChain = fullVowelChain,
            syllableCount = syllables.size,
            stressType = prosody.stressType,
            lastTwoSyllablesPhonetic = lastTwoPhonetic
        )
    }

    fun compareWordsRhyme(word1: String, word2: String): RhymeType {
        val e1 = extractPhoneticEnding(word1)
        val e2 = extractPhoneticEnding(word2)
        if (e1.cleanWord.isEmpty() || e2.cleanWord.isEmpty()) return RhymeType.LIBRE

        val normWord1 = stripAccents(e1.cleanWord)
        val normWord2 = stripAccents(e2.cleanWord)

        if (normWord1 == normWord2) {
            return RhymeType.REPETIDA
        }

        // Check predictable/cliché pair
        if (setOf(normWord1, normWord2) in predictablePairs) {
            return RhymeType.PREVISIBLE
        }

        // Check weak grammatical suffix match (forced rhyme)
        if (weakGrammaticalSuffixes.any { normWord1.endsWith(it) && normWord2.endsWith(it) }) {
            return RhymeType.FORZADA
        }

        // Multisyllabic rhyme: 2+ syllables and last two syllables match phonetically
        if (e1.syllableCount >= 2 && e2.syllableCount >= 2 &&
            e1.lastTwoSyllablesPhonetic.length >= 4 &&
            e1.lastTwoSyllablesPhonetic == e2.lastTwoSyllablesPhonetic
        ) {
            return RhymeType.MULTISILABICA
        }

        // Consonant rhyme
        if (e1.consonantKey.isNotEmpty() && e1.consonantKey == e2.consonantKey) {
            return RhymeType.CONSONANTE
        }

        // Assonant rhyme (identical vowels from the tonic vowel onward)
        if (e1.assonantKey.isNotEmpty() && e1.assonantKey == e2.assonantKey) {
            return RhymeType.ASONANTE
        }

        // Vocalic urban rhyme (last 2+ syllable vowel sequence matches even if stress shifts slightly)
        val v1Tail = e1.vowelPattern.split("-").takeLast(2).joinToString("-")
        val v2Tail = e2.vowelPattern.split("-").takeLast(2).joinToString("-")
        if (v1Tail.length >= 3 && v1Tail == v2Tail) {
            return RhymeType.VOCALICA
        }

        return RhymeType.LIBRE
    }

    fun detectInternalRhymes(line: String): List<String> {
        val words = SpanishMetricaEngine.tokenizeWords(line)
            .filter { it.length >= 3 }
        if (words.size < 2) return emptyList()

        val found = mutableListOf<String>()
        val endings = words.map { extractPhoneticEnding(it) }
        for (i in 0 until endings.size - 1) {
            for (j in i + 1 until endings.size) {
                val w1 = endings[i]
                val w2 = endings[j]
                if (stripAccents(w1.cleanWord) == stripAccents(w2.cleanWord)) continue
                if (w1.consonantKey.length >= 2 && w1.consonantKey == w2.consonantKey) {
                    found.add("${w1.originalWord} ↔ ${w2.originalWord} (consonante)")
                } else if (w1.assonantKey.length >= 3 && w1.assonantKey == w2.assonantKey) {
                    found.add("${w1.originalWord} ↔ ${w2.originalWord} (asonante)")
                }
            }
        }
        return found.distinct()
    }

    fun assignStanzaRhymeLabels(lines: List<String>): List<Pair<String, RhymeType>> {
        val endings = lines.map { line ->
            val lastWord = SpanishMetricaEngine.tokenizeWords(line).lastOrNull() ?: ""
            extractPhoneticEnding(lastWord)
        }

        val labels = MutableList(lines.size) { "-" }
        val types = MutableList(lines.size) { RhymeType.LIBRE }
        var currentLetterChar = 'A'

        for (i in endings.indices) {
            val e1 = endings[i]
            if (e1.cleanWord.isEmpty()) continue

            var matchedIndex = -1
            var bestType = RhymeType.LIBRE

            for (j in 0 until i) {
                val e2 = endings[j]
                if (e2.cleanWord.isEmpty()) continue
                val comparison = compareWordsRhyme(e1.originalWord, e2.originalWord)
                if (comparison != RhymeType.LIBRE) {
                    matchedIndex = j
                    bestType = comparison
                    if (comparison == RhymeType.CONSONANTE || comparison == RhymeType.MULTISILABICA) {
                        break
                    }
                }
            }

            if (matchedIndex >= 0) {
                val letter = if (labels[matchedIndex] == "-") {
                    val assigned = currentLetterChar.toString()
                    labels[matchedIndex] = assigned
                    if (types[matchedIndex] == RhymeType.LIBRE) {
                        types[matchedIndex] = bestType
                    }
                    if (currentLetterChar < 'Z') currentLetterChar++
                    assigned
                } else {
                    labels[matchedIndex]
                }
                labels[i] = letter
                types[i] = bestType
            }
        }

        return labels.zip(types)
    }

    /**
     * Searches rhymes for a target word or a direct vowel pattern (e.g. "a-o", "i-a", "e-o", "o").
     * Supports standard phonetic rhyme search AND "RIMAS POR VOCAL" mode.
     */
    fun searchRhymes(
        query: String,
        vocalicModeOnly: Boolean = false,
        filterSyllables: Int? = null,
        filterStress: StressType? = null
    ): List<RhymeCandidate> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        // Check if user entered a direct vowel pattern like "e-o", "a-a", "i-o", "a o"
        val isDirectVowelQuery = trimmed.all {
            it in baseVowels || it in accentedMap.keys || it == '-' || it == ' ' || it == '/'
        } && trimmed.any { it in baseVowels || it in accentedMap.keys }

        val targetEnding = if (!isDirectVowelQuery) extractPhoneticEnding(trimmed) else null
        val targetVowelSignature = if (isDirectVowelQuery) {
            trimmed.filter { it in baseVowels || it in accentedMap.keys }
                .map { accentedMap[it] ?: it }
                .joinToString("-")
        } else {
            targetEnding?.assonantKey.orEmpty()
        }

        val candidates = mutableListOf<RhymeCandidate>()

        for (entry in curatedLexicon) {
            val candEnding = extractPhoneticEnding(entry.word)
            if (targetEnding != null && stripAccents(candEnding.cleanWord) == stripAccents(targetEnding.cleanWord)) {
                continue
            }
            if (filterSyllables != null && candEnding.syllableCount != filterSyllables) continue
            if (filterStress != null && candEnding.stressType != filterStress) continue

            if (isDirectVowelQuery || vocalicModeOnly) {
                val candVowels = candEnding.assonantKey
                val candPattern = candEnding.vowelPattern
                val candFull = candEnding.fullVowelChain
                val matchesAssonant = candVowels == targetVowelSignature
                val matchesPattern = candPattern.endsWith(targetVowelSignature) || candFull.endsWith(targetVowelSignature)

                if (matchesAssonant || matchesPattern) {
                    val affinity = when {
                        matchesAssonant && candEnding.stressType == targetEnding?.stressType -> 96
                        matchesAssonant -> 90
                        else -> 82
                    }
                    candidates.add(
                        RhymeCandidate(
                            word = entry.word,
                            syllables = candEnding.syllableCount,
                            stressType = candEnding.stressType,
                            rhymeType = if (targetEnding != null && candEnding.consonantKey == targetEnding.consonantKey) {
                                RhymeType.CONSONANTE
                            } else if (matchesAssonant) {
                                RhymeType.ASONANTE
                            } else {
                                RhymeType.VOCALICA
                            },
                            consonantEnding = "-${candEnding.consonantKey}",
                            vowelPattern = candEnding.vowelPattern,
                            registerTag = entry.register,
                            phoneticAffinity = affinity
                        )
                    )
                }
            } else if (targetEnding != null) {
                val rType = compareWordsRhyme(targetEnding.originalWord, entry.word)
                if (rType != RhymeType.LIBRE && rType != RhymeType.REPETIDA) {
                    val affinity = when (rType) {
                        RhymeType.MULTISILABICA -> 99
                        RhymeType.CONSONANTE -> 95
                        RhymeType.ASONANTE -> 86
                        RhymeType.VOCALICA -> 78
                        RhymeType.PREVISIBLE -> 65
                        else -> 70
                    }
                    candidates.add(
                        RhymeCandidate(
                            word = entry.word,
                            syllables = candEnding.syllableCount,
                            stressType = candEnding.stressType,
                            rhymeType = rType,
                            consonantEnding = "-${candEnding.consonantKey}",
                            vowelPattern = candEnding.vowelPattern,
                            registerTag = entry.register,
                            phoneticAffinity = affinity
                        )
                    )
                }
            }
        }

        return candidates.sortedByDescending { it.phoneticAffinity }
    }

    fun normalizeSpanishPhonetics(input: String): String {
        val lower = stripAccents(input.lowercase())
        val sb = StringBuilder()
        var i = 0
        while (i < lower.length) {
            val c = lower[i]
            val next = lower.getOrNull(i + 1)
            when {
                c == 'h' -> {
                    if (i > 0 && lower[i - 1] == 'c') {
                        // handled in 'c'
                    }
                }
                c == 'c' && next == 'h' -> {
                    sb.append("ch")
                    i++
                }
                c == 'l' && next == 'l' -> {
                    sb.append('y')
                    i++
                }
                c == 'r' && next == 'r' -> {
                    sb.append("rr")
                    i++
                }
                c == 'q' && next == 'u' -> {
                    sb.append('k')
                    i++
                }
                c == 'g' && next == 'u' && (lower.getOrNull(i + 2) == 'e' || lower.getOrNull(i + 2) == 'i') -> {
                    sb.append('g')
                    i++
                }
                c == 'c' && (next == 'e' || next == 'i') -> sb.append('s')
                c == 'c' -> sb.append('k')
                c == 'z' -> sb.append('s')
                c == 'v' -> sb.append('b')
                c == 'g' && (next == 'e' || next == 'i') -> sb.append('x')
                c == 'j' -> sb.append('x')
                c == 'y' && i == lower.lastIndex -> sb.append('i')
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    private fun extractPrincipalVowel(syllable: String): String? {
        val clean = syllable.lowercase()
        // Prefer accented vowel if present, else open vowel, else any vowel
        val accented = clean.firstOrNull { it in accentedMap.keys }
        if (accented != null) return accentedMap[accented].toString()
        val open = clean.firstOrNull { it == 'a' || it == 'e' || it == 'o' }
        if (open != null) return open.toString()
        val closed = clean.firstOrNull { it == 'i' || it == 'u' || it == 'ü' || it == 'y' }
        if (closed != null) return if (closed == 'y' || closed == 'ü') (if (closed == 'y') "i" else "u") else closed.toString()
        return null
    }

    fun stripAccents(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(accentedMap[ch] ?: ch)
        }
        return sb.toString()
    }
}
