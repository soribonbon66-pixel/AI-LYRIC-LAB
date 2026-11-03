package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChorusEvaluation
import com.example.model.GlobalSongEvaluation
import com.example.model.LineAnalysis
import com.example.model.SongProject
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.MetricNormal
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioGraphite
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SongEvaluatorScreen(
    project: SongProject,
    sectionAnalyses: Map<String, List<LineAnalysis>>,
    globalEvaluation: GlobalSongEvaluation?,
    chorusEvaluation: ChorusEvaluation? = null,
    onRunFullAiEvaluation: () -> Unit,
    onRunChorusBoost: () -> Unit = {},
    onApplyChorusAlternative: (String) -> Unit = {}
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val allLineAnalyses = project.sections.flatMap { sec ->
        sectionAnalyses[sec.id].orEmpty().filter { it.rawText.isNotBlank() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tab selector: Evaluación Estricta 0-100 vs Laboratorio de Estribillos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioGraphite)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("Auditoría Estricta (0-100)", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricViolet.copy(alpha = 0.35f),
                    selectedLabelColor = NeonCyan,
                    selectedLeadingIconColor = NeonCyan
                ),
                modifier = Modifier.testTag("eval_tab_global")
            )
            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = { Text("Estribillos (Hook Lab)", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.22f),
                    selectedLabelColor = NeonCyan,
                    selectedLeadingIconColor = NeonCyan
                ),
                modifier = Modifier.testTag("eval_tab_chorus")
            )
        }

        if (selectedSubTab == 1) {
            ChorusLabScreen(
                project = project,
                chorusEvaluation = chorusEvaluation,
                onRunChorusBoost = onRunChorusBoost,
                onApplyChorusAlternative = onApplyChorusAlternative
            )
            return@Column
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
                    .border(1.5.dp, ElectricVioletLight, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUDITORÍA LÍRICA Y TÉCNICA EXIGENTE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Criterio adaptado a ${project.config.genre} • ${project.config.bpm} BPM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        val displayScore = globalEvaluation?.overallScore ?: project.lastScore
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(StudioGraphite)
                                .border(3.dp, if (displayScore >= 80) MetricNormal else MetricYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayScore",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (displayScore >= 80) MetricNormal else MetricYellow
                                )
                                Text(
                                    text = "/ 100",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRunFullAiEvaluation,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_full_evaluation_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ejecutar Evaluación Artística y Técnica con IA")
                    }
                }
            }
        }

        // Global AI Evaluation Card if available
        if (globalEvaluation != null) {
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
                            text = "Diagnóstico Global de la Obra",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan
                        )
                        if (globalEvaluation.executiveSummary.isNotBlank()) {
                            Text(
                                text = globalEvaluation.executiveSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        ScoreBarRow("Coherencia narrativa", globalEvaluation.narrativeCoherence, 100)
                        ScoreBarRow("Evolución de la historia", globalEvaluation.storyEvolution, 100)
                        ScoreBarRow("Calidad de las estrofas", globalEvaluation.stanzaQuality, 100)
                        ScoreBarRow("Variedad métrica", globalEvaluation.metricVariety, 100)
                        ScoreBarRow("Variedad de rimas", globalEvaluation.rhymeVariety, 100)
                        ScoreBarRow("Identidad artística", globalEvaluation.artisticIdentity, 100)
                        ScoreBarRow("Originalidad", globalEvaluation.originality, 100)
                        ScoreBarRow("Ausencia de relleno", globalEvaluation.absenceOfFiller, 100)
                        ScoreBarRow("Potencial comercial", globalEvaluation.commercialPotential, 100)
                        ScoreBarRow("Adecuación a ${project.config.genre}", globalEvaluation.genreFit, 100)

                        if (globalEvaluation.highlights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Lo que funciona genial (No tocar):", style = MaterialTheme.typography.titleMedium, color = MetricNormal)
                            globalEvaluation.highlights.forEach { h ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MetricNormal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(h, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        }

                        if (globalEvaluation.actionableFixes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Oportunidades de mejora:", style = MaterialTheme.typography.titleMedium, color = MetricYellow)
                            globalEvaluation.actionableFixes.forEach { fix ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MetricYellow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(fix, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verse-by-Verse Explainable Evaluation (8 Dimensions totaling 100 pts)
        item {
            Text(
                text = "Desglose Técnico Verso a Verso (15+15+15+15+10+10+10+10 = 100)",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        items(allLineAnalyses, key = { it.lineId }) { la ->
            val s = la.score
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
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
                        Text(
                            text = "\"${la.rawText}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${s.totalScore}/100",
                            style = MaterialTheme.typography.titleLarge,
                            color = metricStatusColor(la.metricStatus),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniCriterionChip("Métrica", s.metricScore, 15)
                        MiniCriterionChip("Rima", s.rhymeScore, 15)
                        MiniCriterionChip("Música", s.musicalityScore, 15)
                        MiniCriterionChip("Fluidez", s.fluencyScore, 15)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniCriterionChip("Natural", s.naturalnessScore, 10)
                        MiniCriterionChip("Original", s.originalityScore, 10)
                        MiniCriterionChip("Fuerza", s.expressivePowerScore, 10)
                        MiniCriterionChip("Coherencia", s.contextCoherenceScore, 10)
                    }

                    s.strengths.forEach { st ->
                        Text("✓ $st", style = MaterialTheme.typography.bodyMedium, color = MetricNormal)
                    }
                    s.improvements.forEach { imp ->
                        Text("• $imp", style = MaterialTheme.typography.bodyMedium, color = MetricYellow)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
    }
}

@Composable
private fun MiniCriterionChip(label: String, score: Int, max: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioGraphite)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            text = "$score/$max",
            style = MaterialTheme.typography.labelMedium,
            color = if (score >= max - 2) NeonCyan else TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScoreBarRow(label: String, value: Int, max: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("$value/$max", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / max.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (value >= 80) MetricNormal else ElectricBlue,
            trackColor = StudioGraphite
        )
    }
}

@Composable
fun ChorusLabScreen(
    project: SongProject,
    chorusEvaluation: ChorusEvaluation?,
    onRunChorusBoost: () -> Unit,
    onApplyChorusAlternative: (String) -> Unit
) {
    val chorusSection = project.sections.firstOrNull {
        "chorus" in it.tag.lowercase() || "coro" in it.displayTitle.lowercase() || "estribillo" in it.displayTitle.lowercase()
    } ?: project.sections.firstOrNull()

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANALIZADOR ESPECIAL DE ESTRIBILLOS",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan
                        )
                    }
                    Text(
                        text = "Un gran estribillo no necesita ser complejo: buscamos impacto rítmico, vocales abiertas sonoras, memorabilidad y potencial de coro en vivo o fragmento viral.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (chorusSection != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioGraphite)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Estribillo actual (${chorusSection.tag}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricVioletLight
                                )
                                chorusSection.lines.forEach { line ->
                                    if (line.text.isNotBlank()) {
                                        Text(line.text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onRunChorusBoost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("make_chorus_catchier_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¡HAZLO MÁS PEGADIZO! (Analizar + 5 Hooks)",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        if (chorusEvaluation != null) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Puntuación de Gancho (Hook Score)",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "${chorusEvaluation.overallScore}/100",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MetricNormal
                            )
                        }
                        if (chorusEvaluation.diagnosisSummary.isNotBlank()) {
                            Text(
                                text = chorusEvaluation.diagnosisSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        ScoreBarRow("Facilidad para recordar", chorusEvaluation.memorability, 100)
                        ScoreBarRow("Capacidad de repetición", chorusEvaluation.repeatability, 100)
                        ScoreBarRow("Gancho melódico potencial", chorusEvaluation.melodicHookPotential, 100)
                        ScoreBarRow("Simplicidad y pegada", chorusEvaluation.simplicityAndPunch, 100)
                        ScoreBarRow("Fuerza de la frase principal", chorusEvaluation.mainPhraseStrength, 100)
                        ScoreBarRow("Sonoridad de las vocales", chorusEvaluation.vowelSonority, 100)
                        ScoreBarRow("Ritmo verbal", chorusEvaluation.verbalRhythm, 100)
                        ScoreBarRow("Naturalidad al cantar", chorusEvaluation.naturalness, 100)
                        ScoreBarRow("Participación del público", chorusEvaluation.crowdChantPotential, 100)
                        ScoreBarRow("Potencial fragmento corto (Reels/TikTok)", chorusEvaluation.shortFormViralPotential, 100)

                        if (chorusEvaluation.detectedWarnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Alertas detectadas en el estribillo:", style = MaterialTheme.typography.titleMedium, color = MetricOrange)
                            chorusEvaluation.detectedWarnings.forEach { warn ->
                                Text("• $warn", style = MaterialTheme.typography.bodyMedium, color = MetricYellow)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "5 Versiones Alternativas de Estribillo",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
            }

            items(chorusEvaluation.catchyAlternatives) { altChorus ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricVioletLight.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = altChorus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        OutlinedButton(
                            onClick = { onApplyChorusAlternative(altChorus) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Usar como Estribillo Principal", color = NeonCyan)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
