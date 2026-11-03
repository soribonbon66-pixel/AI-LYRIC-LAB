package com.example

import com.example.domain.SpanishMetricaEngine
import com.example.domain.SpanishRhymeEngine
import com.example.domain.SunoAndStructureGenerator
import com.example.model.ArtisticFreedomMode
import com.example.model.MetricStatus
import com.example.model.RhymeType
import com.example.model.SongConfig
import com.example.model.SongProject
import com.example.model.StressType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `spanish syllabification handles diphthongs and hiatuses accurately`() {
        assertEquals(listOf("co", "ra", "zón"), SpanishMetricaEngine.syllabifyWord("corazón"))
        assertEquals(listOf("cie", "lo"), SpanishMetricaEngine.syllabifyWord("cielo"))
        assertEquals(listOf("po", "e", "ta"), SpanishMetricaEngine.syllabifyWord("poeta"))
        assertEquals(listOf("ma", "rí", "a"), SpanishMetricaEngine.syllabifyWord("María"))
        assertEquals(listOf("a", "ho", "ra"), SpanishMetricaEngine.syllabifyWord("ahora"))
    }

    @Test
    fun `poetic metrica detects synalephas and final stress law`() {
        // "La no-che her-mo-sa" -> 6 grammatical syllables, 1 synalepha (che_her), llana (0) -> 5 poetic syllables
        val res1 = SpanishMetricaEngine.analyzeVerse("La noche hermosa")
        assertEquals(6, res1.grammaticalSyllables)
        assertEquals(1, res1.synalephaCount)
        assertEquals(StressType.LLANA, res1.finalStressType)
        assertEquals(5, res1.poeticSyllables)

        // Final aguda adds +1 poetic syllable ("corazón" ends in acute syllable)
        val resAguda = SpanishMetricaEngine.analyzeVerse("Canta mi corazón")
        assertEquals(StressType.AGUDA, resAguda.finalStressType)
        assertEquals(resAguda.grammaticalSyllables + 1, resAguda.poeticSyllables)

        // Final esdrújula subtracts -1 poetic syllable ("música")
        val resEsdrujula = SpanishMetricaEngine.analyzeVerse("Suena la música")
        assertEquals(StressType.ESDRUJULA, resEsdrujula.finalStressType)
        assertEquals(resEsdrujula.grammaticalSyllables - 1, resEsdrujula.poeticSyllables)
    }

    @Test
    fun `metric status semaphore matches exact specification thresholds for target 10`() {
        val target = 10
        val strict = ArtisticFreedomMode.METRICA_ESTRICTA

        assertEquals(MetricStatus.BLUE, SpanishMetricaEngine.evaluateMetricStatus(7, target, strict))
        assertEquals(MetricStatus.NORMAL, SpanishMetricaEngine.evaluateMetricStatus(10, target, strict))
        assertEquals(MetricStatus.YELLOW, SpanishMetricaEngine.evaluateMetricStatus(11, target, strict))
        assertEquals(MetricStatus.ORANGE, SpanishMetricaEngine.evaluateMetricStatus(12, target, strict))
        assertEquals(MetricStatus.RED, SpanishMetricaEngine.evaluateMetricStatus(14, target, strict))
    }

    @Test
    fun `rhyme engine detects consonant, assonant, predictable, and vocalic rhymes`() {
        // Consonant rhyme (-on)
        assertEquals(
            RhymeType.CONSONANTE,
            SpanishRhymeEngine.compareWordsRhyme("canción", "adicción")
        )

        // Assonant rhyme (i-o: camino / suspiro)
        assertEquals(
            RhymeType.ASONANTE,
            SpanishRhymeEngine.compareWordsRhyme("camino", "suspiro")
        )

        // Predictable cliché pair (corazón / pasión)
        assertEquals(
            RhymeType.PREVISIBLE,
            SpanishRhymeEngine.compareWordsRhyme("corazón", "pasión")
        )

        // Vocalic rhyme search ("e-o")
        val vocalicHits = SpanishRhymeEngine.searchRhymes("e-o", vocalicModeOnly = true)
        assertTrue(vocalicHits.any { it.word == "deseo" || it.word == "veneno" })
    }

    @Test
    fun `suno v6 exporter formats structural tags in english and respects style prompt limit`() {
        val cfg = SongConfig(genre = "Reggaetón clásico", bpm = 94)
        val sections = SunoAndStructureGenerator.buildSmartStructureForConfig(cfg)
        val proj = SongProject(title = "Prueba Suno", config = cfg, sections = sections)

        val pkg = SunoAndStructureGenerator.exportForSunoV6(proj, maxStyleChars = 120)
        assertTrue(pkg.stylePrompt.length <= 120)
        assertTrue(pkg.formattedLyrics.contains("[Intro]"))
        assertTrue(pkg.formattedLyrics.contains("[Chorus]"))

        val pkg1000 = SunoAndStructureGenerator.exportForSunoV6(proj, maxStyleChars = 1000)
        assertTrue(pkg1000.stylePrompt.length <= 1000)
        assertTrue(pkg1000.stylePrompt.contains("94 BPM"))
    }

    @Test
    fun `raw lyrics parser splits pasted song into structured sections automatically`() {
        val rawLyrics = """
            [Verso 1]
            Cruzo la calle bajo la tormenta
            Nadie pregunta lo que el alma intenta
            
            [Estribillo]
            Prende la chispa dentro del motor
            Que ya no queda miedo ni dolor
        """.trimIndent()

        val parsed = SunoAndStructureGenerator.parseRawLyricsTextIntoSections(rawLyrics, defaultTargetSyllables = 11)
        assertEquals(2, parsed.size)
        assertEquals("[Verse 1]", parsed[0].tag)
        assertEquals(2, parsed[0].lines.size)
        assertEquals("[Chorus]", parsed[1].tag)
        assertEquals(2, parsed[1].lines.size)
    }
}
