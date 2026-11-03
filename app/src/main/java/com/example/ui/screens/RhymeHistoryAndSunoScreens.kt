package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CreativeProfilePreferences
import com.example.data.local.PersonalRhymeEntity
import com.example.data.local.SongVersionEntity
import com.example.domain.VersionComparisonReport
import com.example.model.RhymeCandidate
import com.example.model.SongProject
import com.example.model.StressType
import com.example.model.SunoExportPackage
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.MetricNormal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioGraphite
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RhymeAndInspirationScreen(
    rhymeResults: List<RhymeCandidate>,
    inspirationIdeas: List<String>,
    personalRhymes: List<PersonalRhymeEntity>,
    onLocalSearch: (String, Boolean, Int?, StressType?) -> Unit,
    onAiContextualSearch: (String, Boolean) -> Unit,
    onSavePersonalRhyme: (String, String, String, String) -> Unit,
    onDeletePersonalRhyme: (Long) -> Unit
) {
    var query by remember { mutableStateOf("corazón") }
    var vocalicMode by remember { mutableStateOf(false) }
    var filterSyllables by remember { mutableStateOf<Int?>(null) }
    var filterStress by remember { mutableStateOf<StressType?>(null) }

    var newBankPhrase by remember { mutableStateOf("") }
    var newBankFamily by remember { mutableStateOf("") }
    var newBankTag by remember { mutableStateOf("Barra / Rima propia") }
    var newBankNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Motor Fonético de Rimas y Modo Inspiración",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onLocalSearch(it, vocalicMode, filterSyllables, filterStress)
                        },
                        label = { Text("Palabra o patrón vocálico (ej: corazón, madrugada, e-o, i-a)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rhyme_search_input")
                    )

                    // RIMAS POR VOCAL Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Especial: RIMAS POR VOCAL",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricVioletLight
                            )
                            Text(
                                text = "Busca palabras que compartan sonidos vocálicos tónicos aunque cambien las consonantes (clave en Reggaetón, Trap y Rap).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = vocalicMode,
                            onCheckedChange = {
                                vocalicMode = it
                                onLocalSearch(query, it, filterSyllables, filterStress)
                            },
                            modifier = Modifier.testTag("vocalic_rhyme_toggle")
                        )
                    }

                    // Quick Vowel Pattern Presets
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("corazón", "madrugada", "deseo", "camino", "e-o", "a-a", "i-a", "i-o", "u-a").forEach { preset ->
                            FilterChip(
                                selected = query.equals(preset, ignoreCase = true),
                                onClick = {
                                    query = preset
                                    val isVowelPreset = "-" in preset
                                    if (isVowelPreset) vocalicMode = true
                                    onLocalSearch(preset, vocalicMode, filterSyllables, filterStress)
                                },
                                label = { Text(preset) }
                            )
                        }
                    }

                    // Filters by Stress Type & Syllables
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterStress == null,
                            onClick = {
                                filterStress = null
                                onLocalSearch(query, vocalicMode, filterSyllables, null)
                            },
                            label = { Text("Todo acento") }
                        )
                        StressType.entries.forEach { st ->
                            FilterChip(
                                selected = filterStress == st,
                                onClick = {
                                    filterStress = if (filterStress == st) null else st
                                    onLocalSearch(query, vocalicMode, filterSyllables, filterStress)
                                },
                                label = { Text(st.label) }
                            )
                        }
                        listOf(2, 3, 4).forEach { syl ->
                            FilterChip(
                                selected = filterSyllables == syl,
                                onClick = {
                                    filterSyllables = if (filterSyllables == syl) null else syl
                                    onLocalSearch(query, vocalicMode, filterSyllables, filterStress)
                                },
                                label = { Text("$syl síl.") }
                            )
                        }
                    }

                    Button(
                        onClick = { onAiContextualSearch(query, vocalicMode) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_rhyme_inspiration_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ampliar con Rimas Contextuales + Modo Inspiración IA")
                    }
                }
            }
        }

        // Results Grid
        item {
            Text(
                text = "Familias Fonéticas Encontradas (${rhymeResults.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rhymeResults.forEach { cand ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = cand.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cand.syllables} síl · [${cand.vowelPattern}] · ${cand.registerTag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    onSavePersonalRhyme(
                                        cand.word,
                                        "${cand.consonantEnding} [${cand.vowelPattern}]",
                                        cand.registerTag,
                                        "Guardada desde buscador (${cand.rhymeType.label})"
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.BookmarkAdd,
                                    contentDescription = "Guardar en Banco Personal",
                                    tint = ElectricVioletLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Inspiration Ideas from AI
        if (inspirationIdeas.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricVioletLight, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Modo Inspiración: Ideas Conectadas con tu Canción",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricVioletLight
                        )
                        inspirationIdeas.forEach { idea ->
                            Text("• $idea", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Banco Personal de Rimas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Banco Personal de Rimas y Recursos",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    OutlinedTextField(
                        value = newBankPhrase,
                        onValueChange = { newBankPhrase = it },
                        label = { Text("Palabra, pareado o barra creativa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newBankFamily,
                            onValueChange = { newBankFamily = it },
                            label = { Text("Familia fonética (ej. -into)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newBankTag,
                            onValueChange = { newBankTag = it },
                            label = { Text("Categoría") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newBankNotes,
                        onValueChange = { newBankNotes = it },
                        label = { Text("Notas de uso") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newBankPhrase.isNotBlank()) {
                                onSavePersonalRhyme(newBankPhrase, newBankFamily, newBankTag, newBankNotes)
                                newBankPhrase = ""
                                newBankFamily = ""
                                newBankNotes = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar en mi Banco")
                    }
                }
            }
        }

        items(personalRhymes, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.wordOrPhrase, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "${item.phoneticFamily} • ${item.categoryTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                        if (item.notes.isNotBlank()) {
                            Text(item.notes, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = { onDeletePersonalRhyme(item.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = TextMuted)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun VersionHistoryScreen(
    project: SongProject,
    versions: List<SongVersionEntity>,
    comparisonReport: VersionComparisonReport?,
    onSaveSnapshot: (String) -> Unit,
    onRestoreVersion: (SongVersionEntity) -> Unit,
    onCompareStanzas: (String, String, Int) -> Unit
) {
    var snapshotLabel by remember { mutableStateOf("") }
    val firstSectionText = project.sections.firstOrNull()?.lines?.joinToString("\n") { it.text }.orEmpty()
    var versionAText by remember(firstSectionText) { mutableStateOf(firstSectionText) }
    var versionBText by remember {
        mutableStateOf(
            "Cruzo la sombra con el pulso en el motor\n" +
                "Siguiendo el eco que dejaste en el vapor\n" +
                "Finges que nada nos amarra en el salón\n" +
                "Pero tus ojos me confiesan la intención"
        )
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Comparador de Versiones (A vs B)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ElectricVioletLight, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comparador Técnico de Versiones (A vs B)",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan
                        )
                    }
                    Text(
                        text = "Compara dos versiones de una misma estrofa para saber cuál encaja mejor en métrica, rima y respiración.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = versionAText,
                        onValueChange = { versionAText = it },
                        label = { Text("Versión A (Estrofa original)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = versionBText,
                        onValueChange = { versionBText = it },
                        label = { Text("Versión B (Variante alternativa)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCompareStanzas(
                                versionAText,
                                versionBText,
                                project.config.defaultTargetSyllables
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compare_versions_button")
                    ) {
                        Text("Comparar Técnicamente A vs B")
                    }

                    if (comparisonReport != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Ganador técnico: ${comparisonReport.winnerLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MetricNormal
                                )
                                Text(
                                    text = "Puntuación A: ${comparisonReport.versionAScore}/100 · Puntuación B: ${comparisonReport.versionBScore}/100",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                                Text(comparisonReport.metricComparison, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(comparisonReport.rhymeComparison, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(comparisonReport.cadenceComparison, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(comparisonReport.detailedVerdict, style = MaterialTheme.typography.bodyMedium, color = ElectricVioletLight)
                            }
                        }
                    }
                }
            }
        }

        // Manual Snapshot Creator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Historial de Versiones Local (Offline)", style = MaterialTheme.typography.titleLarge)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = snapshotLabel,
                            onValueChange = { snapshotLabel = it },
                            placeholder = { Text("Etiqueta (ej. Antes de cambiar el coro)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onSaveSnapshot(snapshotLabel.ifBlank { "Versión manual" })
                                snapshotLabel = ""
                            }
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }

        items(versions, key = { it.id }) { ver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ver.versionLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "${dateFormat.format(Date(ver.timestamp))} · Calidad: ${ver.scoreSnapshot}/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                    OutlinedButton(onClick = { onRestoreVersion(ver) }) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SunoExportScreen(
    project: SongProject,
    exportPackage: SunoExportPackage?,
    defaultMaxChars: Int,
    onRefreshExport: (Int) -> Unit,
    onUpdateSunoOptions: (com.example.model.SunoVocalOptions) -> Unit = {}
) {
    val context = LocalContext.current
    var maxChars by remember(defaultMaxChars) { mutableIntStateOf(defaultMaxChars.coerceIn(120, 1000)) }
    val sunoOpts = project.config.sunoOptions
    val pkg = remember(project, maxChars, exportPackage) {
        com.example.domain.SunoAndStructureGenerator.exportForSunoV6(project, maxChars)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "EXPORTADOR PRECISO SUNO V6 (100% ENGLISH TAGS)",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "Indicaciones técnicas y etiquetas estructurales generadas en inglés preciso para Suno V6 ([Intro], [Verse 1], [Pre-Chorus], [Chorus], [Bridge], [Outro]) separadas de la letra cantada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Text(
                        text = "Longitud del Style Prompt: ${pkg.stylePrompt.length} / $maxChars caracteres (Máx. 1000)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricVioletLight
                    )
                    Slider(
                        value = maxChars.toFloat(),
                        onValueChange = {
                            maxChars = it.toInt()
                            onRefreshExport(maxChars)
                        },
                        valueRange = 120f..1000f
                    )

                    // Style Prompt Block (English, up to 1000 chars)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioGraphite)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "STYLE OF MUSIC PROMPT (ENGLISH · ${pkg.stylePrompt.length}/1000 CHARS):",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                            Text(
                                text = pkg.stylePrompt,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.testTag("suno_style_prompt_text")
                            )
                        }
                    }

                    // Action Buttons Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                copyToClipboard(context, "Style Prompt", pkg.stylePrompt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_style_prompt_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Style Prompt")
                        }
                        Button(
                            onClick = {
                                copyToClipboard(context, "Letra Suno", pkg.formattedLyrics)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_lyrics_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Letra", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action Buttons Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val full = "=== STYLE PROMPT ===\n${pkg.stylePrompt}\n\n=== LYRICS ===\n${pkg.formattedLyrics}"
                                copyToClipboard(context, "Proyecto Suno Completo", full)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_all_suno_button")
                        ) {
                            Text("Copiar Todo")
                        }
                        OutlinedButton(
                            onClick = {
                                val fullTxt = buildString {
                                    append("PROYECTO LYRIC LAB AI: ${project.title}\n")
                                    append("GÉNERO: ${project.config.genre} (${project.config.bpm} BPM)\n\n")
                                    append("--- SUNO V6 STYLE PROMPT (ENGLISH) ---\n")
                                    append(pkg.stylePrompt).append("\n\n")
                                    append("--- LETRA ESTRUCTURADA ---\n")
                                    append(pkg.formattedLyrics)
                                }
                                shareTextAsFile(context, project.title, fullTxt)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_txt_button")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar TXT")
                        }
                    }
                }
            }
        }

        // Optional Suno V6 Vocal & Production Settings Card (Coros, Backline Vocals, Autotune, Timbre, Peculiaridades)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricVioletLight.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ajustes Opcionales de Voz y Producción para Suno V6",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "Configura coros (backing vocals), estilo vocal, nivel de autotune y peculiaridades. Se inyectan automáticamente en inglés técnico en el Style Prompt y en los corchetes de las estrofas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // 1. Coros / Backing Vocals
                    Text("Coros / Backline Vocals (English Tags):", style = MaterialTheme.typography.labelLarge, color = ElectricVioletLight)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.model.SunoPresets.backingVocalsOptions.forEach { enTag ->
                            FilterChip(
                                selected = sunoOpts.backingVocals == enTag,
                                onClick = {
                                    onUpdateSunoOptions(sunoOpts.copy(backingVocals = enTag))
                                },
                                label = { Text(enTag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // 2. Autotune / Pitch Correction
                    Text("Nivel de Autotune / Pitch Correction:", style = MaterialTheme.typography.labelLarge, color = ElectricVioletLight)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.model.SunoPresets.autotuneOptions.forEach { enTag ->
                            FilterChip(
                                selected = sunoOpts.autotuneLevel == enTag,
                                onClick = {
                                    onUpdateSunoOptions(sunoOpts.copy(autotuneLevel = enTag))
                                },
                                label = { Text(enTag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // 3. Timbre / Estilo Vocal
                    Text("Timbre y Textura Vocal:", style = MaterialTheme.typography.labelLarge, color = ElectricVioletLight)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.model.SunoPresets.vocalTimbreOptions.forEach { enTag ->
                            FilterChip(
                                selected = sunoOpts.vocalTimbre == enTag,
                                onClick = {
                                    onUpdateSunoOptions(sunoOpts.copy(vocalTimbre = enTag))
                                },
                                label = { Text(enTag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // 4. Efectos Vocales
                    Text("Efectos Espaciales de Voz:", style = MaterialTheme.typography.labelLarge, color = ElectricVioletLight)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.model.SunoPresets.vocalEffectsOptions.forEach { enTag ->
                            FilterChip(
                                selected = sunoOpts.vocalEffects == enTag,
                                onClick = {
                                    onUpdateSunoOptions(sunoOpts.copy(vocalEffects = enTag))
                                },
                                label = { Text(enTag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // 5. Otras Peculiaridades Libres
                    OutlinedTextField(
                        value = sunoOpts.customPeculiarities,
                        onValueChange = {
                            onUpdateSunoOptions(sunoOpts.copy(customPeculiarities = it))
                        },
                        label = { Text("Otras peculiaridades (en inglés, ej: female ad-libs, vinyl crackle, 808 glides)") },
                        placeholder = { Text("Ej: talkbox intro, heavy sub-bass drop before chorus, acoustic guitar solo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suno_custom_peculiarities_input")
                    )
                }
            }
        }

        // Formatted Lyrics Preview
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Letra Estructurada con Indicaciones en Inglés (Suno V6)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = pkg.structureSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricVioletLight
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioGraphite)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = pkg.formattedLyrics,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Production Recommendations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Directrices de Producción (English Vocal & Mix Cues)",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                    pkg.productionRecommendations.forEach { rec ->
                        Text("• $rec", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsProfileScreen(
    profile: CreativeProfilePreferences,
    onSaveProfile: (String, String, String, String, Int) -> Unit
) {
    var alias by remember(profile) { mutableStateOf(profile.writerAlias) }
    var style by remember(profile) { mutableStateOf(profile.preferredStyleProfile) }
    var favWords by remember(profile) { mutableStateOf(profile.favoriteWordsCsv) }
    var prefRhyme by remember(profile) { mutableStateOf(profile.preferredRhymeFamily) }
    var sunoChars by remember(profile) { mutableIntStateOf(profile.sunoMaxStyleChars) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aprendizaje Progresivo e Identidad del Compositor",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "Lyric Lab AI recuerda tus expresiones preferidas y sugerencias aceptadas para ayudarte a pulir tu propio sello artístico sin clonar letras iguales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Sugerencias Aceptadas", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${profile.acceptedSuggestionsCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MetricNormal
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Sugerencias Descartadas", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${profile.rejectedSuggestionsCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ElectricVioletLight
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text("Nombre artístico / Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = style,
                        onValueChange = { style = it },
                        label = { Text("Perfil de escritura preferido") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = favWords,
                        onValueChange = { favWords = it },
                        label = { Text("Léxico y palabras aprendidas (separadas por coma)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = prefRhyme,
                        onValueChange = { prefRhyme = it },
                        label = { Text("Preferencia de rimas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveProfile(alias, style, favWords, prefRhyme, sunoChars)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Perfil Creativo")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun shareTextAsFile(context: Context, title: String, content: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$title - Lyric Lab AI")
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Exportar TXT / Compartir Proyecto"))
}
