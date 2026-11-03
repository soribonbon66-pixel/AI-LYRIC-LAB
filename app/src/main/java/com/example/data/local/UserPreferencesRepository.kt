package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lyric_lab_user_profile")

data class CreativeProfilePreferences(
    val writerAlias: String = "Compositor Studio",
    val preferredStyleProfile: String = "Equilibrado (Técnica + Calle)",
    val acceptedSuggestionsCount: Int = 0,
    val rejectedSuggestionsCount: Int = 0,
    val favoriteWordsCsv: String = "madrugada, neón, veneno, pulso, cristal, submundo",
    val preferredRhymeFamily: String = "Vocálica & Multisilábica",
    val autoSaveEnabled: Boolean = true,
    val realTimeAiDebounceMs: Int = 1800,
    val sunoMaxStyleChars: Int = 1000
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val WRITER_ALIAS = stringPreferencesKey("writer_alias")
        val STYLE_PROFILE = stringPreferencesKey("style_profile")
        val ACCEPTED_COUNT = intPreferencesKey("accepted_suggestions_count")
        val REJECTED_COUNT = intPreferencesKey("rejected_suggestions_count")
        val FAVORITE_WORDS = stringPreferencesKey("favorite_words_csv")
        val PREFERRED_RHYME = stringPreferencesKey("preferred_rhyme_family")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_enabled")
        val DEBOUNCE_MS = intPreferencesKey("realtime_ai_debounce_ms")
        val SUNO_MAX_CHARS = intPreferencesKey("suno_max_style_chars")
    }

    val profileFlow: Flow<CreativeProfilePreferences> = context.dataStore.data.map { prefs ->
        CreativeProfilePreferences(
            writerAlias = prefs[Keys.WRITER_ALIAS] ?: "Compositor Studio",
            preferredStyleProfile = prefs[Keys.STYLE_PROFILE] ?: "Equilibrado (Técnica + Calle)",
            acceptedSuggestionsCount = prefs[Keys.ACCEPTED_COUNT] ?: 0,
            rejectedSuggestionsCount = prefs[Keys.REJECTED_COUNT] ?: 0,
            favoriteWordsCsv = prefs[Keys.FAVORITE_WORDS] ?: "madrugada, neón, veneno, pulso, cristal, submundo",
            preferredRhymeFamily = prefs[Keys.PREFERRED_RHYME] ?: "Vocálica & Multisilábica",
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            realTimeAiDebounceMs = prefs[Keys.DEBOUNCE_MS] ?: 1800,
            sunoMaxStyleChars = prefs[Keys.SUNO_MAX_CHARS] ?: 1000
        )
    }

    suspend fun recordSuggestionFeedback(accepted: Boolean, learnedWord: String? = null) {
        context.dataStore.edit { prefs ->
            if (accepted) {
                val cur = prefs[Keys.ACCEPTED_COUNT] ?: 0
                prefs[Keys.ACCEPTED_COUNT] = cur + 1
                if (!learnedWord.isNullOrBlank() && learnedWord.length >= 4) {
                    val existing = (prefs[Keys.FAVORITE_WORDS] ?: "")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toMutableList()
                    if (learnedWord.lowercase() !in existing.map { it.lowercase() }) {
                        existing.add(0, learnedWord)
                        prefs[Keys.FAVORITE_WORDS] = existing.take(20).joinToString(", ")
                    }
                }
            } else {
                val cur = prefs[Keys.REJECTED_COUNT] ?: 0
                prefs[Keys.REJECTED_COUNT] = cur + 1
            }
        }
    }

    suspend fun updateProfile(
        alias: String,
        styleProfile: String,
        favoriteWords: String,
        preferredRhyme: String,
        sunoMaxChars: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WRITER_ALIAS] = alias
            prefs[Keys.STYLE_PROFILE] = styleProfile
            prefs[Keys.FAVORITE_WORDS] = favoriteWords
            prefs[Keys.PREFERRED_RHYME] = preferredRhyme
            prefs[Keys.SUNO_MAX_CHARS] = sunoMaxChars.coerceIn(120, 1000)
        }
    }
}
