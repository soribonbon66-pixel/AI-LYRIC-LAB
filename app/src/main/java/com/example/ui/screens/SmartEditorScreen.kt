package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LineAnalysis
import com.example.model.LineSuggestion
import com.example.model.MetricStatus
import com.example.model.RewriteCandidate
import com.example.model.RewriteMode
import com.example.model.SongProject
import com.example.model.SongSection
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.MetricBlue
import com.example.ui.theme.MetricNormal
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricRed
import com.example.ui.theme.MetricYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RhymeColors
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioGraphite
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

fun metricStatusColor(status: MetricStatus): Color = when (status) {
    MetricStatus.BLUE -> MetricBlue
    MetricStatus.NORMAL -> MetricNormal
    MetricStatus.YELLOW -> MetricYellow
    MetricStatus.ORANGE -> MetricOrange
    MetricStatus.RED -> MetricRed
}

fun rhymeLetterColor(letter: String): Color {
    if (letter == "-" || letter.isBlank()) return TextMuted
    val idx = (letter.first().uppercaseChar() - 'A').coerceIn(0, RhymeColors.lastIndex)
    return RhymeColors[idx]
}

private enum class EditorWorkspaceMode(val label: String) {
    BY_VERSES("Por Versos (Métrica)"),
    FULL_LYRICS("Letra Entera / Pegar")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartEditorScreen(
    project: SongProject,
    sectionAnalyses: Map<String, List<LineAnalysis>>,
    selectedSectionId: String?,
    selectedLineId: String?,
    activeSuggestions: List<LineSuggestion>,
    rewriteCandidates: List<RewriteCandidate>,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectLine: (String, String) -> Unit,
    onUpdateLineText: (String, String, String) -> Unit,
    onSetPronunciationOverride: (String, String, Int?) -> Unit,
    onAddLine: (String, String?) -> Unit,
    onDeleteLine: (String, String) -> Unit,
    onAddSection: (String, String, Int, String) -> Unit,
    onUpdateSectionTarget: (String, Int) -> Unit,
    onDeleteSection: (String) -> Unit,
    onApplySuggestion: (LineSuggestion) -> Unit,
    onDismissSuggestion: (LineSuggestion) -> Unit,
    onRequestAiSuggestions: () -> Unit,
    onRunRewriteTool: (String, RewriteMode, Int) -> Unit,
    onImportFullLyrics: (String) -> Unit,
    onOpenConfigScreen: () -> Unit,
    onToggleCreativeBlockMode: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveSnapshot: (String) -> Unit
) {
    var workspaceMode by remember { mutableStateOf(EditorWorkspaceMode.BY_VERSES) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showRewriteDialog by remember { mutableStateOf(false) }
    var pronunciationDialogLine by remember { mutableStateOf<Pair<String, LineAnalysis>?>(null) }

    val activeSection = project.sections.find { it.id == selectedSectionId } ?: project.sections.firstOrNull()
    val activeLineAnalysis = activeSection?.let { sec ->
        sectionAnalyses[sec.id]?.find { it.lineId == selectedLineId }
            ?: sectionAnalyses[sec.id]?.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Simplified, Intuitive Top Workspace Bar:
        //    Switch between "Por Versos (Métrica)" and "Letra Entera / Pegar", plus Undo/Redo & Config
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioGraphite)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Switcher Pills (Clear & Prominent)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = workspaceMode == EditorWorkspaceMode.BY_VERSES,
                    onClick = { workspaceMode = EditorWorkspaceMode.BY_VERSES },
                    label = {
                        Text(
                            text = "Por Versos",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.38f),
                        selectedLabelColor = NeonCyan,
                        selectedLeadingIconColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("mode_by_verses_chip")
                )

                FilterChip(
                    selected = workspaceMode == EditorWorkspaceMode.FULL_LYRICS,
                    onClick = { workspaceMode = EditorWorkspaceMode.FULL_LYRICS },
                    label = {
                        Text(
                            text = "Letra Entera / Pegar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.22f),
                        selectedLabelColor = NeonCyan,
                        selectedLeadingIconColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("mode_full_lyrics_chip")
                )
            }

            // Right utility actions: Undo, Redo, Save, Config
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(32.dp).testTag("editor_undo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Deshacer",
                        tint = if (canUndo) TextPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(32.dp).testTag("editor_redo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rehacer",
                        tint = if (canRedo) TextPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { onSaveSnapshot("Snapshot manual") },
                    modifier = Modifier.size(32.dp).testTag("editor_save_snapshot_button")
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Guardar versión",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Compact Musical Context & Semaphore Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clickable Genre + BPM Badge (opens Song Config)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceElevated)
                    .clickable { onOpenConfigScreen() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${project.config.genre} · ${project.config.bpm} BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }

            // Strict Score Badge
            val scoreColor = when {
                project.lastScore >= 80 -> MetricNormal
                project.lastScore >= 60 -> MetricYellow
                else -> MetricOrange
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(scoreColor.copy(alpha = 0.16f))
                    .border(1.dp, scoreColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Calidad: ${project.lastScore}/100",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }

            MetricLegendDot(color = MetricBlue, label = "Corto")
            MetricLegendDot(color = MetricNormal, label = "Clavado")
            MetricLegendDot(color = MetricYellow, label = "+1 síl.")
            MetricLegendDot(color = MetricOrange, label = "+2..3")
            MetricLegendDot(color = MetricRed, label = "+4+")

            IconButton(
                onClick = onToggleCreativeBlockMode,
                modifier = Modifier.size(26.dp).testTag("toggle_creative_flow_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DoNotDisturbOn,
                    contentDescription = "Modo bloque creativo sin interrupciones",
                    tint = if (project.config.creativeBlockMode) MetricOrange else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 3. Main Content Area depending on Mode
        when (workspaceMode) {
            EditorWorkspaceMode.FULL_LYRICS -> {
                FullLyricsReadAndImportView(
                    project = project,
                    onApplyFullLyrics = { rawText ->
                        onImportFullLyrics(rawText)
                        workspaceMode = EditorWorkspaceMode.BY_VERSES
                    }
                )
            }

            EditorWorkspaceMode.BY_VERSES -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(2.dp)) }

                    items(project.sections, key = { it.id }) { section ->
                        val analyses = sectionAnalyses[section.id].orEmpty()
                        SectionEditorCard(
                            section = section,
                            analyses = analyses,
                            selectedLineId = selectedLineId,
                            onSelectLine = { lineId -> onSelectLine(section.id, lineId) },
                            onUpdateLineText = { lineId, txt -> onUpdateLineText(section.id, lineId, txt) },
                            onOpenPronunciationDialog = { analysis ->
                                pronunciationDialogLine = section.id to analysis
                            },
                            onAddLine = { afterId -> onAddLine(section.id, afterId) },
                            onDeleteLine = { lineId -> onDeleteLine(section.id, lineId) },
                            onUpdateTargetSyllables = { newTarget -> onUpdateSectionTarget(section.id, newTarget) },
                            onDeleteSection = { onDeleteSection(section.id) },
                            onOpenRewriteForSection = {
                                onSelectLine(section.id, section.lines.firstOrNull()?.id.orEmpty())
                                showRewriteDialog = true
                            }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddSectionDialog = true },
                                modifier = Modifier.weight(1f).testTag("add_section_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nueva Estrofa")
                            }
                            Button(
                                onClick = { showRewriteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.weight(1f).testTag("open_rewriter_button")
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reescribir (12 Modos)")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 4. Bottom Real-Time Diagnostics & Suggestions Dock (Only in By-Verses mode)
                ActiveVerseInspectorDock(
                    analysis = activeLineAnalysis,
                    creativeBlockMode = project.config.creativeBlockMode,
                    suggestions = activeSuggestions,
                    onApplySuggestion = onApplySuggestion,
                    onDismissSuggestion = onDismissSuggestion,
                    onRequestAiSuggestions = onRequestAiSuggestions,
                    onOpenRewriteModal = { showRewriteDialog = true }
                )
            }
        }
    }

    // Pronunciation Selector Modal (Sinalefa vs Hiato vs Sinéresis)
    pronunciationDialogLine?.let { (secId, analysis) ->
        AlertDialog(
            onDismissRequest = { pronunciationDialogLine = null },
            title = { Text("Pronunciación Cantada y Sinalefas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "\"${analysis.rawText}\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                    Text(
                        text = "Sílabas gramaticales: ${analysis.grammaticalSyllables} · Sinalefas detectadas: ${analysis.synalephaCount} · Final: ${analysis.finalStressType.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (analysis.synalephaPairs.isNotEmpty()) {
                        Text(
                            text = "Uniones vocálicas: ${analysis.synalephaPairs.joinToString(", ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricVioletLight
                        )
                    }
                    Text(
                        text = "Elige cómo vas a pronunciar este verso al cantarlo:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    analysis.pronunciationOptions.forEach { opt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetPronunciationOverride(secId, analysis.lineId, opt.syllableCount)
                                    pronunciationDialogLine = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (analysis.poeticSyllables == opt.syllableCount)
                                    ElectricViolet.copy(alpha = 0.28f) else StudioSurfaceElevated
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(opt.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(opt.breakdownPreview, style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                                Text(opt.explanation, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pronunciationDialogLine = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Add Section Modal
    if (showAddSectionDialog) {
        var tag by remember { mutableStateOf("[Verse 2]") }
        var title by remember { mutableStateOf("Verso 2") }
        var targetSyl by remember { mutableIntStateOf(project.config.defaultTargetSyllables) }
        var pattern by remember { mutableStateOf("AABB") }

        AlertDialog(
            onDismissRequest = { showAddSectionDialog = false },
            title = { Text("Nueva Sección Musical") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "[Intro]" to "Intro",
                            "[Verse 1]" to "Verso 1",
                            "[Pre-Chorus]" to "Pre-Estribillo",
                            "[Chorus]" to "Estribillo",
                            "[Verse 2]" to "Verso 2",
                            "[Bridge]" to "Puente",
                            "[Final Chorus]" to "Estribillo Final",
                            "[Outro]" to "Outro"
                        ).forEach { (presetTag, presetTitle) ->
                            FilterChip(
                                selected = tag == presetTag,
                                onClick = {
                                    tag = presetTag
                                    title = presetTitle
                                },
                                label = { Text(presetTag) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre en español") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("Patrón de rima (ej. AABB, ABAB, AAXA)") },
                        singleLine = true
                    )
                    Text("Objetivo silábico por línea: $targetSyl síl.", color = NeonCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { if (targetSyl > 5) targetSyl-- }) { Text("-1 síl.") }
                        OutlinedButton(onClick = { if (targetSyl < 18) targetSyl++ }) { Text("+1 síl.") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddSection(tag, title, targetSyl, pattern)
                    showAddSectionDialog = false
                }) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 12-Mode Rewrite Modal
    if (showRewriteDialog) {
        val currentText = activeLineAnalysis?.rawText?.takeIf { it.isNotBlank() }
            ?: activeSection?.lines?.joinToString("\n") { it.text }.orEmpty()
        val targetSyl = activeSection?.targetSyllables ?: project.config.defaultTargetSyllables

        RewriteToolsModal(
            initialText = currentText,
            targetSyllables = targetSyl,
            candidates = rewriteCandidates,
            onRunRewrite = { text, mode -> onRunRewriteTool(text, mode, targetSyl) },
            onApplyCandidate = { chosenText ->
                val secId = activeSection?.id
                val lId = activeLineAnalysis?.lineId ?: activeSection?.lines?.firstOrNull()?.id
                if (secId != null && lId != null) {
                    onUpdateLineText(secId, lId, chosenText)
                }
                showRewriteDialog = false
            },
            onDismiss = { showRewriteDialog = false }
        )
    }
}

/**
 * Full Lyrics Continuous View & Paste/Import Editor:
 * Lets the songwriter see the entire song at once, copy it, or paste/edit a previously written lyric
 * and convert it automatically into structured sections with prosodic analysis.
 */
@Composable
private fun FullLyricsReadAndImportView(
    project: SongProject,
    onApplyFullLyrics: (String) -> Unit
) {
    val context = LocalContext.current
    val initialText = remember(project.sections) { project.toFullLyricsText() }
    var editableFullText by remember(initialText) { mutableStateOf(initialText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vista de Letra Completa e Importación",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lee toda tu canción de corrido o pega aquí una letra ya escrita (con o sin etiquetas [Verse], [Chorus]) para editarla y medir su métrica.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Quick Clipboard Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isNotBlank()) {
                                editableFullText = clipText
                                Toast.makeText(context, "Letra pegada del portapapeles", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("paste_clipboard_lyrics_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pegar Letra")
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Letra Completa", editableFullText))
                            Toast.makeText(context, "Letra entera copiada", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("copy_full_lyrics_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar Entera")
                    }

                    TextButton(
                        onClick = { editableFullText = "" },
                        modifier = Modifier.testTag("clear_full_lyrics_button")
                    ) {
                        Text("Limpiar", color = TextMuted)
                    }
                }
            }
        }

        // Continuous Full-Song Text Editor
        OutlinedTextField(
            value = editableFullText,
            onValueChange = { editableFullText = it },
            label = { Text("Letra completa de la canción (separa estrofas con una línea en blanco o usa [Verse], [Chorus])") },
            placeholder = {
                Text(
                    "[Verse 1]\nEscribe o pega aquí tu primera estrofa...\n\n[Chorus]\nEscribe o pega aquí tu estribillo..."
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("full_lyrics_text_input")
        )

        Button(
            onClick = { onApplyFullLyrics(editableFullText) },
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_and_analyze_full_lyrics_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aplicar Letra y Analizar Métrica por Versos",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun MetricLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun SectionEditorCard(
    section: SongSection,
    analyses: List<LineAnalysis>,
    selectedLineId: String?,
    onSelectLine: (String) -> Unit,
    onUpdateLineText: (String, String) -> Unit,
    onOpenPronunciationDialog: (LineAnalysis) -> Unit,
    onAddLine: (String?) -> Unit,
    onDeleteLine: (String) -> Unit,
    onUpdateTargetSyllables: (Int) -> Unit,
    onDeleteSection: () -> Unit,
    onOpenRewriteForSection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Clean Section Header: Tag + Title + Target Syllables Stepper + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricViolet.copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = section.tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = section.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Target Syllables compact stepper right in header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(
                        onClick = { onUpdateTargetSyllables(section.targetSyllables - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${section.targetSyllables} síl.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                    TextButton(
                        onClick = { onUpdateTargetSyllables(section.targetSyllables + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onOpenRewriteForSection, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Reescribir sección",
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteSection, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar sección",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simplified, Clean Lines List:
            // Unselected lines show only the text, colored metric underline, and a compact right badge ("11 síl. · A").
            // Selected line expands full phonetic breakdown (sinalefas, stress, vowel pattern, strict score).
            section.lines.forEachIndexed { index, lyricLine ->
                val analysis = analyses.getOrNull(index)
                val isSelected = lyricLine.id == selectedLineId
                val underlineColor = analysis?.let { metricStatusColor(it.metricStatus) } ?: StudioBorder
                val devSign = analysis?.metricDeviation?.let { if (it > 0) "+$it" else "$it" } ?: "0"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) StudioSurfaceElevated else StudioGraphite.copy(alpha = 0.6f))
                        .clickable { onSelectLine(lyricLine.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.width(20.dp)
                        )

                        BasicTextField(
                            value = lyricLine.text,
                            onValueChange = { newText ->
                                onSelectLine(lyricLine.id)
                                onUpdateLineText(lyricLine.id, newText)
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                            cursorBrush = SolidColor(NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lyric_line_input_${section.tag}_$index"),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (lyricLine.text.isEmpty()) {
                                        Text(
                                            text = "Escribe un verso (${section.targetSyllables} síl.)...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Compact summary pill always visible on the right of non-empty lines
                        if (analysis != null && lyricLine.text.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val rColor = rhymeLetterColor(analysis.rhymeFamilyLetter)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(underlineColor.copy(alpha = 0.16f))
                                    .clickable { onOpenPronunciationDialog(analysis) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${analysis.poeticSyllables}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = underlineColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (analysis.rhymeFamilyLetter != "-") {
                                    Text(
                                        text = "· ${analysis.rhymeFamilyLetter}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = rColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isSelected) {
                            IconButton(
                                onClick = { onDeleteLine(lyricLine.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Borrar verso",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Colored Metric Status Underline (<ESTADOS DE MÉTRICA>)
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(underlineColor)
                    )

                    // Expanded Phonetic Details ONLY when this verse is selected
                    AnimatedVisibility(visible = isSelected && analysis != null && lyricLine.text.isNotBlank()) {
                        if (analysis != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(underlineColor.copy(alpha = 0.18f))
                                        .clickable { onOpenPronunciationDialog(analysis) }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${analysis.poeticSyllables} síl. ($devSign) · ${analysis.grammaticalSyllables}g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = underlineColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (analysis.synalephaCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElectricBlue.copy(alpha = 0.2f))
                                            .clickable { onOpenPronunciationDialog(analysis) }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.RecordVoiceOver,
                                                contentDescription = "Sinalefas",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${analysis.synalephaCount}‿ sinalefa",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonCyan
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = analysis.finalStressType.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )

                                if (analysis.vowelSequence.isNotBlank()) {
                                    Text(
                                        text = "Vocales [${analysis.vowelSequence}]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricVioletLight
                                    )
                                }

                                val rColor = rhymeLetterColor(analysis.rhymeFamilyLetter)
                                Text(
                                    text = "${analysis.rhymeType.label} (${analysis.score.totalScore} pts)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = rColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { onAddLine(section.lines.lastOrNull()?.id) },
                modifier = Modifier.testTag("add_line_button_${section.tag}")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir verso", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ActiveVerseInspectorDock(
    analysis: LineAnalysis?,
    creativeBlockMode: Boolean,
    suggestions: List<LineSuggestion>,
    onApplySuggestion: (LineSuggestion) -> Unit,
    onDismissSuggestion: (LineSuggestion) -> Unit,
    onRequestAiSuggestions: () -> Unit,
    onOpenRewriteModal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioGraphite)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            if (analysis == null || analysis.rawText.isBlank()) {
                Text(
                    text = "Toca cualquier verso para ver su diagnóstico métrico o usa 'Letra Entera / Pegar' arriba para meter una canción completa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = analysis.quickDiagnostic,
                            style = MaterialTheme.typography.labelMedium,
                            color = metricStatusColor(analysis.metricStatus),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Nota verso: ${analysis.score.totalScore}/100 · Duración: ${analysis.cadence.estimatedSeconds}s (${analysis.cadence.syllablesPerSecond} síl/s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onOpenRewriteModal,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Reescribir", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = onRequestAiSuggestions,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("request_ai_suggestions_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sugerir IA", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Suggestions Carousel
                AnimatedVisibility(visible = !creativeBlockMode && suggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { sug ->
                            Card(
                                modifier = Modifier
                                    .width(275.dp)
                                    .border(1.dp, ElectricVioletLight.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sug.type.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                        Text(
                                            text = sug.metricDifferenceLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricVioletLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "\"${sug.proposedText}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = sug.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { onDismissSuggestion(sug) },
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Omitir", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                        Button(
                                            onClick = { onApplySuggestion(sug) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MetricNormal),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Usar", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewriteToolsModal(
    initialText: String,
    targetSyllables: Int,
    candidates: List<RewriteCandidate>,
    onRunRewrite: (String, RewriteMode) -> Unit,
    onApplyCandidate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textToRewrite by remember(initialText) { mutableStateOf(initialText) }
    var selectedMode by remember { mutableStateOf(RewriteMode.MANTENER_SIGNIFICADO_MEJORAR_METRICA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reescritura Inteligente (5 Alternativas)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = textToRewrite,
                    onValueChange = { textToRewrite = it },
                    label = { Text("Verso o estrofa a mejorar (Obj: $targetSyllables síl.)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Selecciona el enfoque artístico:", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RewriteMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Button(
                    onClick = { onRunRewrite(textToRewrite, selectedMode) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar 5 Alternativas con IA")
                }

                candidates.forEachIndexed { idx, cand ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApplyCandidate(cand.text) },
                        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Opción ${idx + 1} · ${cand.poeticSyllables} síl. poéticas",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                                Text(
                                    text = cand.rhymeType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricVioletLight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cand.text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            if (cand.rationale.isNotBlank()) {
                                Text(cand.rationale, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca para usar este verso",
                                style = MaterialTheme.typography.labelSmall,
                                color = MetricNormal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
