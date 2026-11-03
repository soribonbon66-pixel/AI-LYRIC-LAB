package com.example.data.local

import com.example.model.ArtisticFreedomMode
import com.example.model.DeliveryStyle
import com.example.model.LyricLine
import com.example.model.RhymeType
import com.example.model.SongConfig
import com.example.model.SongMemory
import com.example.model.SongSection
import com.example.model.SunoVocalOptions
import com.example.model.VoiceType
import org.json.JSONArray
import org.json.JSONObject

object ProjectSerializer {

    fun configToJson(config: SongConfig): String {
        val obj = JSONObject()
        obj.put("genre", config.genre)
        obj.put("subgenre", config.subgenre)
        obj.put("bpm", config.bpm)
        obj.put("timeSignature", config.timeSignature)
        obj.put("mood", config.mood)
        obj.put("language", config.language)
        obj.put("voiceType", config.voiceType.name)
        obj.put("deliveryStyle", config.deliveryStyle.name)
        obj.put("energyIntensity", config.energyIntensity)
        obj.put("lyricComplexity", config.lyricComplexity)
        obj.put("preferredRhymeType", config.preferredRhymeType.name)
        obj.put("artisticFreedom", config.artisticFreedom.name)
        obj.put("defaultTargetSyllables", config.defaultTargetSyllables)
        obj.put("creativeBlockMode", config.creativeBlockMode)

        val sunoObj = JSONObject()
        sunoObj.put("backingVocals", config.sunoOptions.backingVocals)
        sunoObj.put("autotuneLevel", config.sunoOptions.autotuneLevel)
        sunoObj.put("vocalTimbre", config.sunoOptions.vocalTimbre)
        sunoObj.put("vocalEffects", config.sunoOptions.vocalEffects)
        sunoObj.put("customPeculiarities", config.sunoOptions.customPeculiarities)
        obj.put("sunoOptions", sunoObj)
        return obj.toString()
    }

    fun jsonToConfig(json: String): SongConfig {
        if (json.isBlank()) return SongConfig()
        return try {
            val obj = JSONObject(json)
            val sunoObj = obj.optJSONObject("sunoOptions")
            val sunoOptions = if (sunoObj != null) {
                SunoVocalOptions(
                    backingVocals = sunoObj.optString("backingVocals", "Call & Response Ad-libs + Chorus Harmonies"),
                    autotuneLevel = sunoObj.optString("autotuneLevel", "Modern Urban Pitch Polish"),
                    vocalTimbre = sunoObj.optString("vocalTimbre", "Warm Expressive Chest & Head Voice"),
                    vocalEffects = sunoObj.optString("vocalEffects", "Slapback Delay, Wide Stereo Chorus, Crisp De-Esser"),
                    customPeculiarities = sunoObj.optString("customPeculiarities", "Punchy 808 sub-bass, atmospheric intro, dynamic beat drop before chorus")
                )
            } else {
                SunoVocalOptions()
            }

            SongConfig(
                genre = obj.optString("genre", "Reggaetón clásico"),
                subgenre = obj.optString("subgenre", "Perreo melódico"),
                bpm = obj.optInt("bpm", 95),
                timeSignature = obj.optString("timeSignature", "4/4"),
                mood = obj.optString("mood", "Nocturno y magnético"),
                language = obj.optString("language", "Español"),
                voiceType = runCatching { VoiceType.valueOf(obj.optString("voiceType")) }
                    .getOrDefault(VoiceType.INDIFERENTE),
                deliveryStyle = runCatching { DeliveryStyle.valueOf(obj.optString("deliveryStyle")) }
                    .getOrDefault(DeliveryStyle.MELODICA),
                energyIntensity = obj.optInt("energyIntensity", 7),
                lyricComplexity = obj.optString("lyricComplexity", "Equilibrada"),
                preferredRhymeType = runCatching { RhymeType.valueOf(obj.optString("preferredRhymeType")) }
                    .getOrDefault(RhymeType.VOCALICA),
                artisticFreedom = runCatching { ArtisticFreedomMode.valueOf(obj.optString("artisticFreedom")) }
                    .getOrDefault(ArtisticFreedomMode.METRICA_ESTRICTA),
                defaultTargetSyllables = obj.optInt("defaultTargetSyllables", 11),
                creativeBlockMode = obj.optBoolean("creativeBlockMode", false),
                sunoOptions = sunoOptions
            )
        } catch (_: Exception) {
            SongConfig()
        }
    }

    fun memoryToJson(memory: SongMemory): String {
        val obj = JSONObject()
        obj.put("centralTheme", memory.centralTheme)
        obj.put("storyArc", memory.storyArc)
        obj.put("characters", memory.characters)
        obj.put("relationships", memory.relationships)
        obj.put("emotionalTone", memory.emotionalTone)
        obj.put("culturalReferences", memory.culturalReferences)
        obj.put("wordplayAndIrony", memory.wordplayAndIrony)
        obj.put("characteristicVocab", memory.characteristicVocab)
        obj.put("coreMessage", memory.coreMessage)
        return obj.toString()
    }

    fun jsonToMemory(json: String): SongMemory {
        if (json.isBlank()) return SongMemory()
        return try {
            val obj = JSONObject(json)
            SongMemory(
                centralTheme = obj.optString("centralTheme", ""),
                storyArc = obj.optString("storyArc", ""),
                characters = obj.optString("characters", ""),
                relationships = obj.optString("relationships", ""),
                emotionalTone = obj.optString("emotionalTone", ""),
                culturalReferences = obj.optString("culturalReferences", ""),
                wordplayAndIrony = obj.optString("wordplayAndIrony", ""),
                characteristicVocab = obj.optString("characteristicVocab", ""),
                coreMessage = obj.optString("coreMessage", "")
            )
        } catch (_: Exception) {
            SongMemory()
        }
    }

    fun sectionsToJson(sections: List<SongSection>): String {
        val arr = JSONArray()
        for (sec in sections) {
            val secObj = JSONObject()
            secObj.put("id", sec.id)
            secObj.put("tag", sec.tag)
            secObj.put("displayTitle", sec.displayTitle)
            secObj.put("targetSyllables", sec.targetSyllables)
            secObj.put("minSyllables", sec.minSyllables)
            secObj.put("maxSyllables", sec.maxSyllables)
            secObj.put("recommendedLines", sec.recommendedLines)
            secObj.put("rhymePattern", sec.rhymePattern)
            secObj.put("accentScheme", sec.accentScheme)
            secObj.put("vocalDirection", sec.vocalDirection)

            val linesArr = JSONArray()
            for (line in sec.lines) {
                val lObj = JSONObject()
                lObj.put("id", line.id)
                lObj.put("text", line.text)
                if (line.customPoeticSyllablesOverride != null) {
                    lObj.put("overrideSyl", line.customPoeticSyllablesOverride)
                }
                lObj.put("performanceNote", line.performanceNote)
                linesArr.put(lObj)
            }
            secObj.put("lines", linesArr)
            arr.put(secObj)
        }
        return arr.toString()
    }

    fun jsonToSections(json: String): List<SongSection> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SongSection>()
            for (i in 0 until arr.length()) {
                val secObj = arr.getJSONObject(i)
                val linesArr = secObj.optJSONArray("lines") ?: JSONArray()
                val lines = mutableListOf<LyricLine>()
                for (j in 0 until linesArr.length()) {
                    val lObj = linesArr.getJSONObject(j)
                    val overrideVal = if (lObj.has("overrideSyl")) lObj.getInt("overrideSyl") else null
                    lines.add(
                        LyricLine(
                            id = lObj.optString("id", java.util.UUID.randomUUID().toString()),
                            text = lObj.optString("text", ""),
                            customPoeticSyllablesOverride = overrideVal,
                            performanceNote = lObj.optString("performanceNote", "")
                        )
                    )
                }
                list.add(
                    SongSection(
                        id = secObj.optString("id", java.util.UUID.randomUUID().toString()),
                        tag = secObj.optString("tag", "[Verse 1]"),
                        displayTitle = secObj.optString("displayTitle", "Verso 1"),
                        targetSyllables = secObj.optInt("targetSyllables", 11),
                        minSyllables = secObj.optInt("minSyllables", 10),
                        maxSyllables = secObj.optInt("maxSyllables", 12),
                        recommendedLines = secObj.optInt("recommendedLines", 4),
                        rhymePattern = secObj.optString("rhymePattern", "AABB"),
                        accentScheme = secObj.optString("accentScheme", "Fluido"),
                        vocalDirection = secObj.optString("vocalDirection", ""),
                        lines = lines
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
