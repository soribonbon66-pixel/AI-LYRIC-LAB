package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.configureAppCheck
import com.example.ui.AiTaskState
import com.example.ui.AppScreen
import com.example.ui.LyricLabViewModel
import com.example.ui.screens.ChorusLabScreen
import com.example.ui.screens.ProjectsHomeScreen
import com.example.ui.screens.RhymeAndInspirationScreen
import com.example.ui.screens.SettingsProfileScreen
import com.example.ui.screens.SmartEditorScreen
import com.example.ui.screens.SongConfigScreen
import com.example.ui.screens.SongEvaluatorScreen
import com.example.ui.screens.SunoExportScreen
import com.example.ui.screens.VersionHistoryScreen
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.LyricLabTheme
import com.example.ui.theme.MetricNormal
import com.example.ui.theme.MetricRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioGraphite
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            LyricLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBlack
                ) {
                    LyricLabApp()
                }
            }
        }
    }
}

@Composable
fun LyricLabApp(
    viewModel: LyricLabViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val sectionAnalyses by viewModel.sectionAnalyses.collectAsStateWithLifecycle()
    val selectedSectionId by viewModel.selectedSectionId.collectAsStateWithLifecycle()
    val selectedLineId by viewModel.selectedLineId.collectAsStateWithLifecycle()
    val activeSuggestions by viewModel.activeSuggestions.collectAsStateWithLifecycle()
    val rewriteCandidates by viewModel.rewriteCandidates.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val globalEvaluation by viewModel.globalEvaluation.collectAsStateWithLifecycle()
    val chorusEvaluation by viewModel.chorusEvaluation.collectAsStateWithLifecycle()
    val rhymeResults by viewModel.rhymeSearchResults.collectAsStateWithLifecycle()
    val inspirationIdeas by viewModel.inspirationIdeas.collectAsStateWithLifecycle()
    val personalRhymes by viewModel.personalRhymes.collectAsStateWithLifecycle()
    val versions by viewModel.projectVersions.collectAsStateWithLifecycle()
    val comparisonReport by viewModel.comparisonReport.collectAsStateWithLifecycle()
    val sunoExport by viewModel.sunoExport.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != AppScreen.EDITOR && currentScreen != AppScreen.HOME) {
        viewModel.navigateTo(AppScreen.EDITOR)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = StudioBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioGraphite)
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LYRIC LAB AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${activeProject.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.CONFIG) },
                            modifier = Modifier.size(36.dp).testTag("top_nav_config")
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Configuración y Memoria de la canción",
                                tint = if (currentScreen == AppScreen.CONFIG) NeonCyan else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.VERSION_HISTORY) },
                            modifier = Modifier.size(36.dp).testTag("top_nav_history")
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Historial y Comparador A/B",
                                tint = if (currentScreen == AppScreen.VERSION_HISTORY) NeonCyan else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                            modifier = Modifier.size(36.dp).testTag("top_nav_settings")
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil creativo",
                                tint = if (currentScreen == AppScreen.SETTINGS) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                // AI Status Banner
                AnimatedVisibility(visible = aiTaskState !is AiTaskState.Idle) {
                    val bannerColor = when (aiTaskState) {
                        is AiTaskState.Loading -> ElectricViolet.copy(alpha = 0.25f)
                        is AiTaskState.Success -> MetricNormal.copy(alpha = 0.2f)
                        is AiTaskState.Error -> MetricRed.copy(alpha = 0.22f)
                        AiTaskState.Idle -> StudioSurfaceElevated
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bannerColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (aiTaskState is AiTaskState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                            } else {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val text = when (val st = aiTaskState) {
                                is AiTaskState.Loading -> st.label
                                is AiTaskState.Success -> st.message
                                is AiTaskState.Error -> st.errorMessage
                                AiTaskState.Idle -> ""
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearAiStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar aviso",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioGraphite,
                contentColor = TextPrimary
            ) {
                val navItems = listOf(
                    Triple(AppScreen.HOME, "Canciones", Icons.Default.LibraryMusic),
                    Triple(AppScreen.EDITOR, "Editor", Icons.Default.EditNote),
                    Triple(AppScreen.RHYME_FINDER, "Rimas", Icons.Default.Search),
                    Triple(AppScreen.EVALUATOR, "Evaluar", Icons.Default.Analytics),
                    Triple(AppScreen.SUNO_EXPORT, "Suno V6", Icons.Default.IosShare)
                )
                navItems.forEach { (screen, label, icon) ->
                    val selected = currentScreen == screen ||
                        (screen == AppScreen.EVALUATOR && currentScreen == AppScreen.CHORUS_LAB)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("bottom_nav_${screen.name}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> ProjectsHomeScreen(
                    projects = allProjects,
                    activeProjectId = activeProject.id,
                    searchQuery = searchQuery,
                    onlyFavorites = onlyFavorites,
                    onSearchChange = viewModel::setSearchQuery,
                    onToggleFavorites = viewModel::toggleOnlyFavorites,
                    onSelectProject = { proj ->
                        viewModel.loadProjectIntoWorkspace(proj)
                        viewModel.navigateTo(AppScreen.EDITOR)
                    },
                    onCreateNewProject = { title, cfg, mem, pasted ->
                        viewModel.createNewSongProject(title, cfg, mem, pasted)
                    },
                    onDuplicateProject = viewModel::duplicateSongProject,
                    onDeleteProject = viewModel::deleteSongProject,
                    onToggleFavoriteProject = viewModel::toggleFavoriteProject,
                    onRenameProject = { proj, newName ->
                        viewModel.loadProjectIntoWorkspace(proj)
                        viewModel.renameActiveProject(newName)
                    }
                )

                AppScreen.CONFIG -> SongConfigScreen(
                    project = activeProject,
                    onUpdateConfig = viewModel::updateSongConfig,
                    onUpdateMemory = viewModel::updateSongMemory,
                    onGenerateAiStructure = viewModel::generateSmartStructureWithAi,
                    onApplyLocalStructure = viewModel::applyLocalTemplateStructure,
                    onInferMemoryWithAi = viewModel::inferSongMemoryFromLyrics,
                    onNavigateToEditor = { viewModel.navigateTo(AppScreen.EDITOR) }
                )

                AppScreen.EDITOR -> SmartEditorScreen(
                    project = activeProject,
                    sectionAnalyses = sectionAnalyses,
                    selectedSectionId = selectedSectionId,
                    selectedLineId = selectedLineId,
                    activeSuggestions = activeSuggestions,
                    rewriteCandidates = rewriteCandidates,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onSelectLine = viewModel::selectLine,
                    onUpdateLineText = viewModel::updateLineText,
                    onSetPronunciationOverride = viewModel::setLineSyllablePronunciationOverride,
                    onAddLine = viewModel::addLineToSection,
                    onDeleteLine = viewModel::deleteLineFromSection,
                    onAddSection = viewModel::addSection,
                    onUpdateSectionTarget = viewModel::updateSectionTargetSyllables,
                    onDeleteSection = viewModel::deleteSection,
                    onApplySuggestion = viewModel::applySuggestion,
                    onDismissSuggestion = viewModel::dismissSuggestion,
                    onRequestAiSuggestions = viewModel::requestAiSuggestionsForSelectedLine,
                    onRunRewriteTool = viewModel::runRewriteTool,
                    onImportFullLyrics = viewModel::importOrUpdateFullLyricsText,
                    onOpenConfigScreen = { viewModel.navigateTo(AppScreen.CONFIG) },
                    onToggleCreativeBlockMode = {
                        val cur = activeProject.config
                        viewModel.updateSongConfig(cur.copy(creativeBlockMode = !cur.creativeBlockMode))
                    },
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onSaveSnapshot = viewModel::saveManualVersionSnapshot
                )

                AppScreen.EVALUATOR -> SongEvaluatorScreen(
                    project = activeProject,
                    sectionAnalyses = sectionAnalyses,
                    globalEvaluation = globalEvaluation,
                    chorusEvaluation = chorusEvaluation,
                    onRunFullAiEvaluation = viewModel::runFullSongAiEvaluation,
                    onRunChorusBoost = viewModel::runChorusAnalyzerAndBoost,
                    onApplyChorusAlternative = { alt ->
                        viewModel.replaceChorusWithAlternative(alt)
                        viewModel.navigateTo(AppScreen.EDITOR)
                    }
                )

                AppScreen.CHORUS_LAB -> ChorusLabScreen(
                    project = activeProject,
                    chorusEvaluation = chorusEvaluation,
                    onRunChorusBoost = viewModel::runChorusAnalyzerAndBoost,
                    onApplyChorusAlternative = { alt ->
                        viewModel.replaceChorusWithAlternative(alt)
                        viewModel.navigateTo(AppScreen.EDITOR)
                    }
                )

                AppScreen.RHYME_FINDER -> RhymeAndInspirationScreen(
                    rhymeResults = rhymeResults,
                    inspirationIdeas = inspirationIdeas,
                    personalRhymes = personalRhymes,
                    onLocalSearch = viewModel::performLocalRhymeSearch,
                    onAiContextualSearch = viewModel::performAiContextualRhymeAndInspiration,
                    onSavePersonalRhyme = viewModel::saveToPersonalRhymeBank,
                    onDeletePersonalRhyme = viewModel::deleteFromPersonalRhymeBank
                )

                AppScreen.VERSION_HISTORY -> VersionHistoryScreen(
                    project = activeProject,
                    versions = versions,
                    comparisonReport = comparisonReport,
                    onSaveSnapshot = viewModel::saveManualVersionSnapshot,
                    onRestoreVersion = { ver ->
                        viewModel.restoreVersionSnapshot(ver)
                        viewModel.navigateTo(AppScreen.EDITOR)
                    },
                    onCompareStanzas = viewModel::compareTwoStanzas
                )

                AppScreen.SUNO_EXPORT -> SunoExportScreen(
                    project = activeProject,
                    exportPackage = sunoExport,
                    defaultMaxChars = userProfile.sunoMaxStyleChars,
                    onRefreshExport = viewModel::refreshSunoExport,
                    onUpdateSunoOptions = viewModel::updateSunoVocalOptions
                )

                AppScreen.SETTINGS -> SettingsProfileScreen(
                    profile = userProfile,
                    onSaveProfile = viewModel::saveUserProfilePreferences
                )
            }
        }
    }
}
