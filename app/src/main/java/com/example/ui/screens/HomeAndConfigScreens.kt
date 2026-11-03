package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ArtisticFreedomMode
import com.example.model.DeliveryStyle
import com.example.model.PredefinedGenres
import com.example.model.RhymeType
import com.example.model.SongConfig
import com.example.model.SongMemory
import com.example.model.SongProject
import com.example.model.VoiceType
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

@Composable
fun ProjectsHomeScreen(
    projects: List<SongProject>,
    activeProjectId: Long,
    searchQuery: String,
    onlyFavorites: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onSelectProject: (SongProject) -> Unit,
    onCreateNewProject: (String, SongConfig, SongMemory, String) -> Unit,
    onDuplicateProject: (SongProject) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onToggleFavoriteProject: (SongProject) -> Unit,
    onRenameProject: (SongProject, String) -> Unit
) {
    var showNewDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<SongProject?>(null) }

    val filtered = remember(projects, searchQuery, onlyFavorites) {
        projects.filter { proj ->
            val matchesQuery = searchQuery.isBlank() ||
                proj.title.contains(searchQuery, ignoreCase = true) ||
                proj.config.genre.contains(searchQuery, ignoreCase = true) ||
                proj.sections.any { sec -> sec.lines.any { it.text.contains(searchQuery, ignoreCase = true) } }
            val matchesFav = !onlyFavorites || proj.isFavorite
            matchesQuery && matchesFav
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Studio Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.38f),
                                StudioSurfaceElevated,
                                ElectricBlue.copy(alpha = 0.28f)
                            )
                        )
                    )
                    .border(1.dp, ElectricVioletLight.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Estudio Lyric Lab AI",
                                tint = NeonCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ESTUDIO DE COMPOSICIÓN",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan
                            )
                        }
                        Button(
                            onClick = { showNewDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("new_song_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva canción", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nueva / Importar Letra")
                        }
                    }
                    Text(
                        text = "Crea una canción desde cero o pega una letra ya escrita para medir su métrica, mejorar rimas y preparar Suno V6.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Buscar por título, género o verso...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_projects_input")
                )
                FilterChip(
                    selected = onlyFavorites,
                    onClick = onToggleFavorites,
                    label = { Text("Favoritos") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (onlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Filtrar favoritos",
                            tint = if (onlyFavorites) NeonCyan else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("filter_favorites_chip")
                )
            }
        }

        items(filtered, key = { it.id }) { proj ->
            val isSelected = proj.id == activeProjectId
            val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
            val lineCount = proj.sections.sumOf { sec -> sec.lines.count { it.text.isNotBlank() } }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) NeonCyan else StudioBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectProject(proj) }
                    .testTag("project_card_${proj.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) StudioSurfaceElevated else StudioSurface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = proj.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeonCyan.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVA",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${proj.config.genre} • ${proj.config.subgenre} • ${proj.config.bpm} BPM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElectricVioletLight
                            )
                        }

                        // Score Badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StudioGraphite)
                                .border(1.5.dp, MetricNormal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${proj.lastScore}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MetricNormal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val previewVerse = proj.sections.flatMap { it.lines }
                        .firstOrNull { it.text.isNotBlank() }?.text
                        ?: "Sin letra todavía — pulsa para abrir en el Editor Inteligente."
                    Text(
                        text = "\"$previewVerse\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${proj.sections.size} secciones · $lineCount versos · Mod: ${dateFormat.format(Date(proj.updatedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Row {
                            IconButton(onClick = { onToggleFavoriteProject(proj) }) {
                                Icon(
                                    imageVector = if (proj.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (proj.isFavorite) NeonCyan else TextSecondary
                                )
                            }
                            IconButton(onClick = { projectToRename = proj }) {
                                Icon(Icons.Default.Edit, contentDescription = "Renombrar", tint = TextSecondary)
                            }
                            IconButton(onClick = { onDuplicateProject(proj) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = TextSecondary)
                            }
                            if (projects.size > 1) {
                                IconButton(onClick = { onDeleteProject(proj.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showNewDialog) {
        NewSongDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { title, genre, subgenre, bpm, mood, theme, pastedLyrics ->
                val (_, defaultSyl, defaultDeliv) = PredefinedGenres.defaultConfigForGenre(genre)
                val cfg = SongConfig(
                    genre = genre,
                    subgenre = subgenre,
                    bpm = bpm,
                    mood = mood,
                    deliveryStyle = defaultDeliv,
                    defaultTargetSyllables = defaultSyl
                )
                val mem = SongMemory(
                    centralTheme = theme,
                    emotionalTone = mood
                )
                showNewDialog = false
                onCreateNewProject(title, cfg, mem, pastedLyrics)
            }
        )
    }

    projectToRename?.let { target ->
        var newName by remember(target) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Renombrar proyecto") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Título de la canción") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameProject(target, newName)
                    projectToRename = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun NewSongDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Reggaetón clásico") }
    var subgenre by remember { mutableStateOf("Perreo melódico") }
    var bpm by remember { mutableIntStateOf(95) }
    var mood by remember { mutableStateOf("Nocturno, magnético y directo") }
    var theme by remember { mutableStateOf("") }
    var initialLyrics by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Canción o Importar Letra") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la canción") },
                    placeholder = { Text("Ej: Eclipse en la Pista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = {
                        genre = it
                        val (defBpm, _, _) = PredefinedGenres.defaultConfigForGenre(it)
                        bpm = defBpm
                    },
                    label = { Text("Género musical (libre o predefinido)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PredefinedGenres.allGenres.take(8).forEach { g ->
                        FilterChip(
                            selected = genre.equals(g, ignoreCase = true),
                            onClick = {
                                genre = g
                                val (defBpm, _, _) = PredefinedGenres.defaultConfigForGenre(g)
                                bpm = defBpm
                            },
                            label = { Text(g) }
                        )
                    }
                }
                OutlinedTextField(
                    value = subgenre,
                    onValueChange = { subgenre = it },
                    label = { Text("Subgénero / matiz sonoro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("BPM: $bpm", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { bpm = it.toInt() },
                    valueRange = 50f..200f
                )
                OutlinedTextField(
                    value = mood,
                    onValueChange = { mood = it },
                    label = { Text("Mood / Estado emocional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialLyrics,
                    onValueChange = { initialLyrics = it },
                    label = { Text("Pegar letra ya escrita (opcional)") },
                    placeholder = { Text("Si ya tienes una letra escrita, pégala aquí y la dividiremos en estrofas automáticamente...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_song_paste_lyrics_input")
                )
                OutlinedTextField(
                    value = theme,
                    onValueChange = { theme = it },
                    label = { Text("Idea o historia central (opcional)") },
                    placeholder = { Text("¿De qué trata la canción?") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { "Canción ${genre.take(12)}" },
                        genre,
                        subgenre,
                        bpm,
                        mood,
                        theme,
                        initialLyrics
                    )
                },
                modifier = Modifier.testTag("confirm_create_song_button")
            ) {
                Text(if (initialLyrics.isNotBlank()) "Importar y Analizar" else "Crear y Estructurar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongConfigScreen(
    project: SongProject,
    onUpdateConfig: (SongConfig) -> Unit,
    onUpdateMemory: (SongMemory) -> Unit,
    onGenerateAiStructure: () -> Unit,
    onApplyLocalStructure: () -> Unit,
    onInferMemoryWithAi: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val config = project.config
    val memory = project.memory

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Identidad Musical y Género Libre",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    OutlinedTextField(
                        value = config.genre,
                        onValueChange = { onUpdateConfig(config.copy(genre = it)) },
                        label = { Text("Género musical (escribe cualquiera o elige abajo)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("config_genre_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PredefinedGenres.allGenres.forEach { preset ->
                            val selected = config.genre.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val (defBpm, defSyl, defDeliv) = PredefinedGenres.defaultConfigForGenre(preset)
                                    onUpdateConfig(
                                        config.copy(
                                            genre = preset,
                                            bpm = defBpm,
                                            defaultTargetSyllables = defSyl,
                                            deliveryStyle = defDeliv
                                        )
                                    )
                                },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet.copy(alpha = 0.35f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = config.subgenre,
                            onValueChange = { onUpdateConfig(config.copy(subgenre = it)) },
                            label = { Text("Subgénero") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = config.language,
                            onValueChange = { onUpdateConfig(config.copy(language = it)) },
                            label = { Text("Idioma") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    OutlinedTextField(
                        value = config.mood,
                        onValueChange = { onUpdateConfig(config.copy(mood = it)) },
                        label = { Text("Mood / Estado emocional y atmósfera") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // BPM, Compás & Interpretación
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricVioletLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. BPM, Cadencia, Voz y Libertad Artística",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tempo: ${config.bpm} BPM", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("4/4", "3/4", "6/8").forEach { sig ->
                                FilterChip(
                                    selected = config.timeSignature == sig,
                                    onClick = { onUpdateConfig(config.copy(timeSignature = sig)) },
                                    label = { Text(sig) }
                                )
                            }
                        }
                    }

                    Slider(
                        value = config.bpm.toFloat(),
                        onValueChange = { onUpdateConfig(config.copy(bpm = it.toInt())) },
                        valueRange = 45f..210f
                    )

                    Text(
                        text = "Sílabas métricas objetivo base: ${config.defaultTargetSyllables} síl./verso",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Slider(
                        value = config.defaultTargetSyllables.toFloat(),
                        onValueChange = { onUpdateConfig(config.copy(defaultTargetSyllables = it.toInt())) },
                        valueRange = 6f..16f,
                        steps = 9
                    )

                    Text("Tipo de Voz:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceType.entries.forEach { vt ->
                            FilterChip(
                                selected = config.voiceType == vt,
                                onClick = { onUpdateConfig(config.copy(voiceType = vt)) },
                                label = { Text(vt.label) }
                            )
                        }
                    }

                    Text("Estilo de Interpretación y Cadencia:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DeliveryStyle.entries.forEach { ds ->
                            FilterChip(
                                selected = config.deliveryStyle == ds,
                                onClick = { onUpdateConfig(config.copy(deliveryStyle = ds)) },
                                label = { Text(ds.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text("Tipo de Rima Preferida:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            RhymeType.CONSONANTE,
                            RhymeType.ASONANTE,
                            RhymeType.VOCALICA,
                            RhymeType.MULTISILABICA,
                            RhymeType.LIBRE
                        ).forEach { rt ->
                            FilterChip(
                                selected = config.preferredRhymeType == rt,
                                onClick = { onUpdateConfig(config.copy(preferredRhymeType = rt)) },
                                label = { Text(rt.label) }
                            )
                        }
                    }

                    Text("Control de Libertad Artística:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArtisticFreedomMode.entries.forEach { af ->
                            FilterChip(
                                selected = config.artisticFreedom == af,
                                onClick = { onUpdateConfig(config.copy(artisticFreedom = af)) },
                                label = { Text(af.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Bloque Creativo (Sin interrupciones)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Desactiva sugerencias automáticas mientras escribes para fluir sin distracciones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = config.creativeBlockMode,
                            onCheckedChange = { onUpdateConfig(config.copy(creativeBlockMode = it)) }
                        )
                    }
                }
            }
        }

        // GENERADOR INTELIGENTE DE ESTRUCTURAS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ElectricVioletLight, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Generador Inteligente de Estructura",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Calcula estrofas, número de líneas, patrón silábico, esquema de rimas y variaciones métricas adaptadas a ${config.genre} (${config.bpm} BPM).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onGenerateAiStructure()
                                onNavigateToEditor()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_structure_ai_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GENERAR ESTRUCTURA IA")
                        }
                        OutlinedButton(
                            onClick = {
                                onApplyLocalStructure()
                                onNavigateToEditor()
                            },
                            modifier = Modifier.testTag("generate_structure_local_button")
                        ) {
                            Text("Plantilla Instantánea")
                        }
                    }
                }
            }
        }

        // MEMORIA DE LA CANCIÓN (Inteligencia Contextual)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4. Memoria de la Canción (Contexto IA)",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        TextButton(onClick = onInferMemoryWithAi) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deducir de la letra")
                        }
                    }
                    Text(
                        text = "La IA consulta esta memoria antes de sugerir cambios para respetar tu historia, personajes, ironía y jerga.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = memory.centralTheme,
                        onValueChange = { onUpdateMemory(memory.copy(centralTheme = it)) },
                        label = { Text("Tema central") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memory.storyArc,
                        onValueChange = { onUpdateMemory(memory.copy(storyArc = it)) },
                        label = { Text("Historia y evolución narrativa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = memory.characters,
                            onValueChange = { onUpdateMemory(memory.copy(characters = it)) },
                            label = { Text("Personajes / Actitud") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = memory.emotionalTone,
                            onValueChange = { onUpdateMemory(memory.copy(emotionalTone = it)) },
                            label = { Text("Tono narrativo") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = memory.characteristicVocab,
                        onValueChange = { onUpdateMemory(memory.copy(characteristicVocab = it)) },
                        label = { Text("Vocabulario característico, jerga y referencias") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memory.wordplayAndIrony,
                        onValueChange = { onUpdateMemory(memory.copy(wordplayAndIrony = it)) },
                        label = { Text("Ironía, dobles sentidos o recursos clave") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memory.coreMessage,
                        onValueChange = { onUpdateMemory(memory.copy(coreMessage = it)) },
                        label = { Text("Mensaje principal que quieres transmitir") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
