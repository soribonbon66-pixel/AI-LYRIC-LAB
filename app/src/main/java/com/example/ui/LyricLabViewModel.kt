package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiLyricAiService
import com.example.data.ai.LyricAiService
import com.example.data.local.CreativeProfilePreferences
import com.example.data.local.LyricDatabase
import com.example.data.local.PersonalRhymeEntity
import com.example.data.local.ProjectSerializer
import com.example.data.local.SongVersionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.LyricRepository
import com.example.domain.CadenceAndQualityAnalyzer
import com.example.domain.SpanishRhymeEngine
import com.example.domain.SunoAndStructureGenerator
import com.example.domain.VersionComparisonReport
import com.example.model.ChorusEvaluation
import com.example.model.GlobalSongEvaluation
import com.example.model.LineAnalysis
import com.example.model.LineSuggestion
import com.example.model.LyricLine
import com.example.model.RewriteCandidate
import com.example.model.RewriteMode
import com.example.model.RhymeCandidate
import com.example.model.SongConfig
import com.example.model.SongMemory
import com.example.model.SongProject
import com.example.model.SongSection
import com.example.model.StressType
import com.example.model.SunoExportPackage
import com.example.model.SunoVocalOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    HOME("Mis Canciones"),
    CONFIG("Configuración Musical"),
    EDITOR("Editor Inteligente"),
    EVALUATOR("Evaluación 0-100"),
    CHORUS_LAB("Laboratorio de Estribillos"),
    RHYME_FINDER("Buscador de Rimas & Inspiración"),
    VERSION_HISTORY("Historial y Comparador"),
    SUNO_EXPORT("Exportador Suno V6"),
    SETTINGS("Perfil Creativo y Ajustes")
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Loading(val label: String) : AiTaskState
    data class Success(val message: String) : AiTaskState
    data class Error(val errorMessage: String) : AiTaskState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LyricLabViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LyricLabViewModel"
    }

    private val database = LyricDatabase.getInstance(application)
    private val repository = LyricRepository(database.lyricDao())
    private val prefsRepository = UserPreferencesRepository(application)
    private val aiService: LyricAiService = GeminiLyricAiService()

    private val _currentScreen = MutableStateFlow(AppScreen.EDITOR)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val allProjects: StateFlow<List<SongProject>> = repository.allProjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalRhymes: StateFlow<List<PersonalRhymeEntity>> = repository.personalRhymesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<CreativeProfilePreferences> = prefsRepository.profileFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CreativeProfilePreferences()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _activeProject = MutableStateFlow(SongProject())
    val activeProject: StateFlow<SongProject> = _activeProject.asStateFlow()

    val projectVersions: StateFlow<List<SongVersionEntity>> = _activeProject
        .flatMapLatest { proj ->
            if (proj.id > 0L) repository.getVersionsFlow(proj.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time local analysis per sectionId
    private val _sectionAnalyses = MutableStateFlow<Map<String, List<LineAnalysis>>>(emptyMap())
    val sectionAnalyses: StateFlow<Map<String, List<LineAnalysis>>> = _sectionAnalyses.asStateFlow()

    // Currently selected line & section in the Smart Editor
    private val _selectedSectionId = MutableStateFlow<String?>(null)
    val selectedSectionId: StateFlow<String?> = _selectedSectionId.asStateFlow()

    private val _selectedLineId = MutableStateFlow<String?>(null)
    val selectedLineId: StateFlow<String?> = _selectedLineId.asStateFlow()

    // Undo / Redo Stacks for sections
    private val undoStack = ArrayDeque<List<SongSection>>()
    private val redoStack = ArrayDeque<List<SongSection>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Suggestions & Rewrites
    private val _activeSuggestions = MutableStateFlow<List<LineSuggestion>>(emptyList())
    val activeSuggestions: StateFlow<List<LineSuggestion>> = _activeSuggestions.asStateFlow()

    private val _rewriteCandidates = MutableStateFlow<List<RewriteCandidate>>(emptyList())
    val rewriteCandidates: StateFlow<List<RewriteCandidate>> = _rewriteCandidates.asStateFlow()

    // Evaluations
    private val _globalEvaluation = MutableStateFlow<GlobalSongEvaluation?>(null)
    val globalEvaluation: StateFlow<GlobalSongEvaluation?> = _globalEvaluation.asStateFlow()

    private val _chorusEvaluation = MutableStateFlow<ChorusEvaluation?>(null)
    val chorusEvaluation: StateFlow<ChorusEvaluation?> = _chorusEvaluation.asStateFlow()

    // Rhyme Search & Inspiration
    private val _rhymeSearchResults = MutableStateFlow<List<RhymeCandidate>>(emptyList())
    val rhymeSearchResults: StateFlow<List<RhymeCandidate>> = _rhymeSearchResults.asStateFlow()

    private val _inspirationIdeas = MutableStateFlow<List<String>>(emptyList())
    val inspirationIdeas: StateFlow<List<String>> = _inspirationIdeas.asStateFlow()

    // Version comparison report
    private val _comparisonReport = MutableStateFlow<VersionComparisonReport?>(null)
    val comparisonReport: StateFlow<VersionComparisonReport?> = _comparisonReport.asStateFlow()

    // Suno Export Package
    private val _sunoExport = MutableStateFlow<SunoExportPackage?>(null)
    val sunoExport: StateFlow<SunoExportPackage?> = _sunoExport.asStateFlow()

    // AI Status
    private val _aiTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiTaskState: StateFlow<AiTaskState> = _aiTaskState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var realtimeSuggestionJob: Job? = null

    init {
        viewModelScope.launch {
            val initial = repository.ensureInitialSeedData()
            loadProjectIntoWorkspace(initial)
            // Initial default rhyme search
            performLocalRhymeSearch("corazón", vocalicMode = false, syllables = null, stress = null)
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.SUNO_EXPORT) {
            refreshSunoExport(userProfile.value.sunoMaxStyleChars)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleOnlyFavorites() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun clearAiStatus() {
        _aiTaskState.value = AiTaskState.Idle
    }

    fun loadProjectIntoWorkspace(project: SongProject) {
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoFlags()
        _activeProject.value = project
        val firstSection = project.sections.firstOrNull()
        _selectedSectionId.value = firstSection?.id
        _selectedLineId.value = firstSection?.lines?.firstOrNull()?.id
        recomputeAllLocalAnalyses(project)
        refreshSunoExport(userProfile.value.sunoMaxStyleChars)
    }

    fun createNewSongProject(
        title: String,
        config: SongConfig,
        memory: SongMemory,
        initialPastedLyrics: String = ""
    ) {
        viewModelScope.launch {
            var created = repository.createNewProject(title, config, memory)
            if (initialPastedLyrics.isNotBlank()) {
                val parsedSections = SunoAndStructureGenerator.parseRawLyricsTextIntoSections(
                    rawText = initialPastedLyrics,
                    defaultTargetSyllables = config.defaultTargetSyllables
                )
                created = repository.saveProject(
                    created.copy(sections = parsedSections),
                    saveVersionLabel = "Letra importada inicial"
                )
            }
            loadProjectIntoWorkspace(created)
            _currentScreen.value = AppScreen.EDITOR
        }
    }

    /**
     * Parses and imports a full continuous lyric text (pasted or edited in the "Letra Entera" view)
     * into structured sections and recomputes all prosodic/metric analyses immediately.
     */
    fun importOrUpdateFullLyricsText(rawFullText: String) {
        if (rawFullText.isBlank()) return
        pushUndoState()
        val currentProj = _activeProject.value
        val parsedSections = SunoAndStructureGenerator.parseRawLyricsTextIntoSections(
            rawText = rawFullText,
            defaultTargetSyllables = currentProj.config.defaultTargetSyllables
        )
        val updatedProj = currentProj.copy(sections = parsedSections)
        _activeProject.value = updatedProj
        val firstSec = parsedSections.firstOrNull()
        _selectedSectionId.value = firstSec?.id
        _selectedLineId.value = firstSec?.lines?.firstOrNull()?.id
        recomputeAllLocalAnalyses(updatedProj)
        refreshSunoExport(userProfile.value.sunoMaxStyleChars)
        scheduleAutoSave(updatedProj, snapshotLabel = "Letra entera actualizada/importada")
        _aiTaskState.value = AiTaskState.Success("Letra procesada en ${parsedSections.size} estrofas y analizada verso a verso.")
    }

    fun updateSunoVocalOptions(newOptions: SunoVocalOptions) {
        val currentProj = _activeProject.value
        val updatedConfig = currentProj.config.copy(sunoOptions = newOptions)
        val updatedProj = currentProj.copy(config = updatedConfig)
        _activeProject.value = updatedProj
        refreshSunoExport(userProfile.value.sunoMaxStyleChars)
        scheduleAutoSave(updatedProj, snapshotLabel = null)
    }

    fun duplicateSongProject(project: SongProject) {
        viewModelScope.launch {
            val copy = repository.duplicateProject(project)
            loadProjectIntoWorkspace(copy)
        }
    }

    fun deleteSongProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            val remaining = allProjects.value.filter { it.id != projectId }
            if (_activeProject.value.id == projectId && remaining.isNotEmpty()) {
                loadProjectIntoWorkspace(remaining.first())
            }
        }
    }

    fun renameActiveProject(newTitle: String) {
        if (newTitle.isBlank()) return
        val updated = _activeProject.value.copy(title = newTitle.trim())
        _activeProject.value = updated
        scheduleAutoSave(updated, snapshotLabel = null)
    }

    fun toggleFavoriteProject(project: SongProject) {
        viewModelScope.launch {
            val toggled = project.copy(isFavorite = !project.isFavorite)
            val saved = repository.saveProject(toggled)
            if (_activeProject.value.id == saved.id) {
                _activeProject.value = saved
            }
        }
    }

    fun updateSongConfig(newConfig: SongConfig) {
        val updated = _activeProject.value.copy(config = newConfig)
        _activeProject.value = updated
        recomputeAllLocalAnalyses(updated)
        refreshSunoExport(userProfile.value.sunoMaxStyleChars)
        scheduleAutoSave(updated, snapshotLabel = null)
    }

    fun updateSongMemory(newMemory: SongMemory) {
        val updated = _activeProject.value.copy(memory = newMemory)
        _activeProject.value = updated
        scheduleAutoSave(updated, snapshotLabel = null)
    }

    fun selectLine(sectionId: String, lineId: String) {
        _selectedSectionId.value = sectionId
        _selectedLineId.value = lineId
        updateLocalSuggestionsForSelection(sectionId, lineId)
    }

    fun updateLineText(sectionId: String, lineId: String, newText: String) {
        pushUndoState()
        val currentProj = _activeProject.value
        val updatedSections = currentProj.sections.map { sec ->
            if (sec.id != sectionId) sec
            else sec.copy(
                lines = sec.lines.map { line ->
                    if (line.id != lineId) line
                    else line.copy(text = newText, customPoeticSyllablesOverride = null)
                }
            )
        }
        val updatedProj = currentProj.copy(sections = updatedSections)
        _activeProject.value = updatedProj
        recomputeAllLocalAnalyses(updatedProj)
        updateLocalSuggestionsForSelection(sectionId, lineId)
        scheduleAutoSave(updatedProj, snapshotLabel = null)

        // Trigger debounced AI suggestion only if not in creative block mode and text has enough words
        if (!updatedProj.config.creativeBlockMode && newText.trim().split(Regex("\\s+")).size >= 4) {
            scheduleDebouncedAiSuggestions(sectionId, lineId, newText)
        }
    }

    fun setLineSyllablePronunciationOverride(sectionId: String, lineId: String, syllableOverride: Int?) {
        pushUndoState()
        val currentProj = _activeProject.value
        val updatedSections = currentProj.sections.map { sec ->
            if (sec.id != sectionId) sec
            else sec.copy(
                lines = sec.lines.map { line ->
                    if (line.id != lineId) line
                    else line.copy(customPoeticSyllablesOverride = syllableOverride)
                }
            )
        }
        val updatedProj = currentProj.copy(sections = updatedSections)
        _activeProject.value = updatedProj
        recomputeAllLocalAnalyses(updatedProj)
        scheduleAutoSave(updatedProj, snapshotLabel = null)
    }

    fun addLineToSection(sectionId: String, afterLineId: String? = null) {
        pushUndoState()
        val currentProj = _activeProject.value
        val newLine = LyricLine(text = "")
        val updatedSections = currentProj.sections.map { sec ->
            if (sec.id != sectionId) sec
            else {
                val mutable = sec.lines.toMutableList()
                val idx = if (afterLineId != null) mutable.indexOfFirst { it.id == afterLineId } else -1
                if (idx >= 0) {
                    mutable.add(idx + 1, newLine)
                } else {
                    mutable.add(newLine)
                }
                sec.copy(lines = mutable)
            }
        }
        val updatedProj = currentProj.copy(sections = updatedSections)
        _activeProject.value = updatedProj
        _selectedSectionId.value = sectionId
        _selectedLineId.value = newLine.id
        recomputeAllLocalAnalyses(updatedProj)
        scheduleAutoSave(updatedProj, snapshotLabel = null)
    }

    fun deleteLineFromSection(sectionId: String, lineId: String) {
        pushUndoState()
        val currentProj = _activeProject.value
        val updatedSections = currentProj.sections.map { sec ->
            if (sec.id != sectionId) sec
            else {
                val filtered = sec.lines.filterNot { it.id == lineId }
                sec.copy(lines = filtered.ifEmpty { listOf(LyricLine(text = "")) })
            }
        }
        val updatedProj = currentProj.copy(sections = updatedSections)
        _activeProject.value = updatedProj
        recomputeAllLocalAnalyses(updatedProj)
        scheduleAutoSave(updatedProj, snapshotLabel = null)
    }

    fun addSection(tag: String, displayTitle: String, targetSyllables: Int, rhymePattern: String) {
        pushUndoState()
        val currentProj = _activeProject.value
        val newSec = SongSection(
            tag = if (tag.startsWith("[")) tag else "[$tag]",
            displayTitle = displayTitle.ifBlank { tag },
            targetSyllables = targetSyllables,
            minSyllables = (targetSyllables - 1).coerceAtLeast(4),
            maxSyllables = targetSyllables + 1,
            recommendedLines = 4,
            rhymePattern = rhymePattern.ifBlank { "AABB" },
            lines = listOf(
                LyricLine(text = ""),
                LyricLine(text = "")
            )
        )
        val updatedProj = currentProj.copy(sections = currentProj.sections + newSec)
        _activeProject.value = updatedProj
        _selectedSectionId.value = newSec.id
        _selectedLineId.value = newSec.lines.first().id
        recomputeAllLocalAnalyses(updatedProj)
        scheduleAutoSave(updatedProj, snapshotLabel = "Añadida sección ${newSec.tag}")
    }

    fun updateSectionTargetSyllables(sectionId: String, newTarget: Int) {
        val safeTarget = newTarget.coerceIn(4, 22)
        val currentProj = _activeProject.value
        val updatedSections = currentProj.sections.map { sec ->
            if (sec.id != sectionId) sec
            else sec.copy(
                targetSyllables = safeTarget,
                minSyllables = (safeTarget - 1).coerceAtLeast(4),
                maxSyllables = safeTarget + 1
            )
        }
        val updatedProj = currentProj.copy(sections = updatedSections)
        _activeProject.value = updatedProj
        recomputeAllLocalAnalyses(updatedProj)
        scheduleAutoSave(updatedProj, snapshotLabel = null)
    }

    fun deleteSection(sectionId: String) {
        if (_activeProject.value.sections.size <= 1) return
        pushUndoState()
        val currentProj = _activeProject.value
        val updatedProj = currentProj.copy(
            sections = currentProj.sections.filterNot { it.id == sectionId }
        )
        _activeProject.value = updatedProj
        recomputeAllLocalAnalyses(updatedProj)
        scheduleAutoSave(updatedProj, snapshotLabel = null)
    }

    fun applySuggestion(suggestion: LineSuggestion) {
        val secId = _selectedSectionId.value ?: return
        val lId = _selectedLineId.value ?: return
        updateLineText(secId, lId, suggestion.proposedText)
        viewModelScope.launch {
            val lastWord = com.example.domain.SpanishMetricaEngine
                .tokenizeWords(suggestion.proposedText)
                .lastOrNull()
            prefsRepository.recordSuggestionFeedback(accepted = true, learnedWord = lastWord)
        }
    }

    fun dismissSuggestion(suggestion: LineSuggestion) {
        _activeSuggestions.value = _activeSuggestions.value.filterNot { it.id == suggestion.id }
        viewModelScope.launch {
            prefsRepository.recordSuggestionFeedback(accepted = false)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _activeProject.value.sections
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        val updated = _activeProject.value.copy(sections = previous)
        _activeProject.value = updated
        updateUndoRedoFlags()
        recomputeAllLocalAnalyses(updated)
        scheduleAutoSave(updated, snapshotLabel = null)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _activeProject.value.sections
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        val updated = _activeProject.value.copy(sections = next)
        _activeProject.value = updated
        updateUndoRedoFlags()
        recomputeAllLocalAnalyses(updated)
        scheduleAutoSave(updated, snapshotLabel = null)
    }

    fun saveManualVersionSnapshot(label: String) {
        viewModelScope.launch {
            val proj = _activeProject.value
            val saved = repository.saveProject(
                proj,
                saveVersionLabel = label.ifBlank { "Snapshot manual" }
            )
            _activeProject.value = saved
            _aiTaskState.value = AiTaskState.Success("Versión guardada en el historial local.")
        }
    }

    fun restoreVersionSnapshot(version: SongVersionEntity) {
        pushUndoState()
        val restoredSections = ProjectSerializer.jsonToSections(version.sectionsJson)
        if (restoredSections.isNotEmpty()) {
            val updated = _activeProject.value.copy(
                sections = restoredSections,
                lastScore = version.scoreSnapshot
            )
            _activeProject.value = updated
            recomputeAllLocalAnalyses(updated)
            scheduleAutoSave(updated, snapshotLabel = "Restaurada: ${version.versionLabel}")
        }
    }

    // --- AI OPERATIONS (GEMINI VIA FIREBASE AI LOGIC) ---

    fun generateSmartStructureWithAi() {
        val proj = _activeProject.value
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Diseñando estructura musical con IA para ${proj.config.genre} (${proj.config.bpm} BPM)...")
            try {
                val aiSections = aiService.generateSmartStructure(proj.config, proj.memory)
                if (aiSections.isNotEmpty()) {
                    pushUndoState()
                    val updated = proj.copy(sections = aiSections)
                    _activeProject.value = updated
                    recomputeAllLocalAnalyses(updated)
                    scheduleAutoSave(updated, snapshotLabel = "Estructura generada con IA")
                    _aiTaskState.value = AiTaskState.Success("Estructura inteligente generada para ${proj.config.genre}.")
                } else {
                    _aiTaskState.value = AiTaskState.Error("El modelo devolvió una estructura vacía.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Structure generation timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo de espera agotado (60s) al generar la estructura.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Structure generation failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Error al generar estructura con IA.")
            }
        }
    }

    fun applyLocalTemplateStructure() {
        pushUndoState()
        val proj = _activeProject.value
        val sections = SunoAndStructureGenerator.buildSmartStructureForConfig(proj.config)
        val updated = proj.copy(sections = sections)
        _activeProject.value = updated
        recomputeAllLocalAnalyses(updated)
        scheduleAutoSave(updated, snapshotLabel = "Estructura base por género")
    }

    fun requestAiSuggestionsForSelectedLine() {
        val proj = _activeProject.value
        val secId = _selectedSectionId.value ?: proj.sections.firstOrNull()?.id ?: return
        val section = proj.sections.find { it.id == secId } ?: return
        val line = section.lines.find { it.id == _selectedLineId.value } ?: section.lines.firstOrNull() ?: return
        if (line.text.isBlank()) return

        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Analizando verso en contexto con Gemini...")
            try {
                val aiSugs = aiService.generateRealTimeSuggestions(
                    lineText = line.text,
                    targetSyllables = section.targetSyllables,
                    section = section,
                    config = proj.config,
                    memory = proj.memory,
                    profile = userProfile.value
                )
                val localSugs = _activeSuggestions.value
                _activeSuggestions.value = (aiSugs + localSugs).distinctBy { it.proposedText }
                _aiTaskState.value = AiTaskState.Success("Sugerencias contextuales listas.")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Line suggestion timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo agotado al consultar sugerencias IA.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Line suggestion failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Error al obtener sugerencias IA.")
            }
        }
    }

    fun runRewriteTool(textToRewrite: String, mode: RewriteMode, targetSyllables: Int) {
        if (textToRewrite.isBlank()) return
        val proj = _activeProject.value
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Generando 5 alternativas (${mode.label})...")
            try {
                val candidates = aiService.rewriteLines(
                    originalText = textToRewrite,
                    mode = mode,
                    targetSyllables = targetSyllables,
                    config = proj.config,
                    memory = proj.memory,
                    profile = userProfile.value
                )
                _rewriteCandidates.value = candidates
                _aiTaskState.value = AiTaskState.Success("5 alternativas generadas con análisis métrico.")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Rewrite timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo agotado (60s) al reescribir.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Rewrite failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "No se pudo completar la reescritura IA.")
            }
        }
    }

    fun runFullSongAiEvaluation() {
        val proj = _activeProject.value
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Evaluando canción completa (0-100) según ${proj.config.genre}...")
            try {
                val eval = aiService.evaluateFullSong(proj)
                _globalEvaluation.value = eval
                val updated = proj.copy(lastScore = eval.overallScore)
                _activeProject.value = updated
                scheduleAutoSave(updated, snapshotLabel = "Evaluación IA (${eval.overallScore}/100)")
                _aiTaskState.value = AiTaskState.Success("Evaluación global completada: ${eval.overallScore}/100.")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Song evaluation timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo agotado al evaluar la canción.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Song evaluation failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Error en la evaluación con IA.")
            }
        }
    }

    fun runChorusAnalyzerAndBoost() {
        val proj = _activeProject.value
        val chorusSec = proj.sections.firstOrNull {
            "chorus" in it.tag.lowercase() || "coro" in it.displayTitle.lowercase() || "estribillo" in it.displayTitle.lowercase()
        } ?: proj.sections.firstOrNull() ?: return

        val chorusLines = chorusSec.lines.map { it.text }.filter { it.isNotBlank() }
        if (chorusLines.isEmpty()) return

        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Analizando gancho comercial y creando 5 estribillos más pegadizos...")
            try {
                val evaluation = aiService.evaluateAndBoostChorus(
                    chorusLines = chorusLines,
                    config = proj.config,
                    memory = proj.memory
                )
                _chorusEvaluation.value = evaluation
                _aiTaskState.value = AiTaskState.Success("Análisis de estribillo completado (${evaluation.overallScore}/100).")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chorus boost timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo agotado al analizar el estribillo.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chorus boost failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Error al potenciar el estribillo.")
            }
        }
    }

    fun replaceChorusWithAlternative(alternativeChorusText: String) {
        pushUndoState()
        val proj = _activeProject.value
        val newLines = alternativeChorusText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { LyricLine(text = it) }
        if (newLines.isEmpty()) return

        val chorusIndex = proj.sections.indexOfFirst {
            "chorus" in it.tag.lowercase() || "estribillo" in it.displayTitle.lowercase()
        }
        val updatedSections = if (chorusIndex >= 0) {
            proj.sections.mapIndexed { idx, sec ->
                if (idx == chorusIndex) sec.copy(lines = newLines) else sec
            }
        } else {
            proj.sections + SongSection(
                tag = "[Chorus]",
                displayTitle = "Estribillo",
                targetSyllables = proj.config.defaultTargetSyllables - 1,
                lines = newLines
            )
        }
        val updated = proj.copy(sections = updatedSections)
        _activeProject.value = updated
        recomputeAllLocalAnalyses(updated)
        scheduleAutoSave(updated, snapshotLabel = "Estribillo actualizado desde Laboratorio")
    }

    fun inferSongMemoryFromLyrics() {
        val proj = _activeProject.value
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Actualizando Memoria de la Canción desde los versos...")
            try {
                val inferred = aiService.inferAndUpdateSongMemory(proj)
                val updated = proj.copy(memory = inferred)
                _activeProject.value = updated
                scheduleAutoSave(updated, snapshotLabel = null)
                _aiTaskState.value = AiTaskState.Success("Memoria contextual actualizada.")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Memory inference timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo agotado al analizar la memoria.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Memory inference failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Error al deducir la memoria de la canción.")
            }
        }
    }

    fun performLocalRhymeSearch(
        query: String,
        vocalicMode: Boolean,
        syllables: Int?,
        stress: StressType?
    ) {
        val results = SpanishRhymeEngine.searchRhymes(
            query = query,
            vocalicModeOnly = vocalicMode,
            filterSyllables = syllables,
            filterStress = stress
        )
        _rhymeSearchResults.value = results
    }

    fun performAiContextualRhymeAndInspiration(query: String, vocalicMode: Boolean) {
        if (query.isBlank()) return
        val proj = _activeProject.value
        // Run local immediately first
        performLocalRhymeSearch(query, vocalicMode, null, null)

        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Loading("Buscando rimas contextuales e ideas para \"$query\"...")
            try {
                val (aiRhymes, ideas) = aiService.findContextualRhymesAndInspiration(
                    queryOrConcept = query,
                    vocalicMode = vocalicMode,
                    config = proj.config,
                    memory = proj.memory
                )
                val merged = (aiRhymes + _rhymeSearchResults.value).distinctBy { it.word.lowercase() }
                _rhymeSearchResults.value = merged
                _inspirationIdeas.value = ideas
                _aiTaskState.value = AiTaskState.Success("Rimas e ideas de inspiración generadas.")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Inspiration timed out", e)
                _aiTaskState.value = AiTaskState.Error("Tiempo agotado al buscar inspiración con IA.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Inspiration failed", e)
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Error en búsqueda creativa con IA.")
            }
        }
    }

    fun saveToPersonalRhymeBank(wordOrPhrase: String, phoneticFamily: String, category: String, notes: String) {
        if (wordOrPhrase.isBlank()) return
        viewModelScope.launch {
            repository.addPersonalRhyme(wordOrPhrase.trim(), phoneticFamily.trim(), category.trim(), notes.trim())
        }
    }

    fun deleteFromPersonalRhymeBank(id: Long) {
        viewModelScope.launch {
            repository.deletePersonalRhyme(id)
        }
    }

    fun compareTwoStanzas(versionA: String, versionB: String, targetSyllables: Int) {
        val report = SunoAndStructureGenerator.compareStanzaVersions(
            versionAText = versionA,
            versionBText = versionB,
            targetSyllables = targetSyllables,
            config = _activeProject.value.config
        )
        _comparisonReport.value = report
    }

    fun refreshSunoExport(maxStyleChars: Int) {
        _sunoExport.value = SunoAndStructureGenerator.exportForSunoV6(
            project = _activeProject.value,
            maxStyleChars = maxStyleChars
        )
    }

    fun saveUserProfilePreferences(
        alias: String,
        styleProfile: String,
        favoriteWords: String,
        preferredRhyme: String,
        sunoMaxChars: Int
    ) {
        viewModelScope.launch {
            prefsRepository.updateProfile(alias, styleProfile, favoriteWords, preferredRhyme, sunoMaxChars)
            refreshSunoExport(sunoMaxChars)
        }
    }

    // --- INTERNAL HELPERS ---

    private fun recomputeAllLocalAnalyses(project: SongProject) {
        val map = mutableMapOf<String, List<LineAnalysis>>()
        var allScoresSum = 0
        var scoredLineCount = 0

        for (sec in project.sections) {
            val lineAnalyses = CadenceAndQualityAnalyzer.analyzeSection(sec, project.config)
            map[sec.id] = lineAnalyses
            for (la in lineAnalyses) {
                if (la.rawText.isNotBlank()) {
                    allScoresSum += la.score.totalScore
                    scoredLineCount++
                }
            }
        }
        _sectionAnalyses.value = map

        if (scoredLineCount > 0) {
            val computedAvg = (allScoresSum.toDouble() / scoredLineCount).toInt().coerceIn(0, 100)
            if (computedAvg != project.lastScore) {
                _activeProject.value = project.copy(lastScore = computedAvg)
            }
        }

        val curSecId = _selectedSectionId.value ?: project.sections.firstOrNull()?.id
        val curLineId = _selectedLineId.value ?: project.sections.firstOrNull()?.lines?.firstOrNull()?.id
        if (curSecId != null && curLineId != null) {
            updateLocalSuggestionsForSelection(curSecId, curLineId)
        }
    }

    private fun updateLocalSuggestionsForSelection(sectionId: String, lineId: String) {
        val sec = _activeProject.value.sections.find { it.id == sectionId } ?: return
        val line = sec.lines.find { it.id == lineId } ?: return
        val analysis = _sectionAnalyses.value[sectionId]?.find { it.lineId == lineId } ?: return
        if (!_activeProject.value.config.creativeBlockMode) {
            _activeSuggestions.value = CadenceAndQualityAnalyzer.generateInstantLocalSuggestions(line, analysis)
        } else {
            _activeSuggestions.value = emptyList()
        }
    }

    private fun scheduleAutoSave(project: SongProject, snapshotLabel: String?) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(550L)
            val saved = repository.saveProject(project, saveVersionLabel = snapshotLabel)
            if (_activeProject.value.id == 0L && saved.id != 0L) {
                _activeProject.value = _activeProject.value.copy(id = saved.id)
            }
        }
    }

    private fun scheduleDebouncedAiSuggestions(sectionId: String, lineId: String, text: String) {
        realtimeSuggestionJob?.cancel()
        realtimeSuggestionJob = viewModelScope.launch {
            val debounceMs = userProfile.value.realTimeAiDebounceMs.toLong().coerceIn(1200L, 4000L)
            delay(debounceMs)
            val proj = _activeProject.value
            if (proj.config.creativeBlockMode) return@launch
            val section = proj.sections.find { it.id == sectionId } ?: return@launch
            try {
                val aiSugs = aiService.generateRealTimeSuggestions(
                    lineText = text,
                    targetSyllables = section.targetSyllables,
                    section = section,
                    config = proj.config,
                    memory = proj.memory,
                    profile = userProfile.value
                )
                if (_selectedLineId.value == lineId) {
                    val existing = _activeSuggestions.value
                    _activeSuggestions.value = (aiSugs + existing).distinctBy { it.proposedText }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Silent background fallback on debounced keystroke so we never interrupt typing
                Log.d(TAG, "Background debounced AI suggestion skipped: ${e.message}")
            }
        }
    }

    private fun pushUndoState() {
        val current = _activeProject.value.sections
        if (undoStack.lastOrNull() != current) {
            if (undoStack.size >= 30) undoStack.removeFirst()
            undoStack.addLast(current)
            redoStack.clear()
            updateUndoRedoFlags()
        }
    }

    private fun updateUndoRedoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
