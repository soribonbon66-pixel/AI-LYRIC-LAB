package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Studio Dark Palette (Obsidian, Graphite, Electric Violet, Electric Blue, Neon Cyan)
val StudioBlack = Color(0xFF090A0F)
val StudioGraphite = Color(0xFF12141E)
val StudioSurface = Color(0xFF191C2A)
val StudioSurfaceElevated = Color(0xFF222638)
val StudioBorder = Color(0xFF2E334B)

val ElectricViolet = Color(0xFF9D4EDD)
val ElectricVioletLight = Color(0xFFC77DFF)
val ElectricBlue = Color(0xFF3A86FF)
val NeonCyan = Color(0xFF00F5D4)
val StudioMagenta = Color(0xFFFF2A85)

val TextPrimary = Color(0xFFF4F6FB)
val TextSecondary = Color(0xFFA3A9C2)
val TextMuted = Color(0xFF6C728F)

// Metric Semaphore Colors (<ESTADOS DE MÉTRICA>)
val MetricBlue = Color(0xFF38B6FF)      // AZUL: Menos sílabas de las recomendadas
val MetricNormal = Color(0xFF00E676)    // NORMAL: Dentro del margen métrico establecido
val MetricYellow = Color(0xFFFFEA00)    // AMARILLO: Exceso leve de sílabas (+1)
val MetricOrange = Color(0xFFFF9100)    // NARANJA: Exceso moderado de sílabas (+2..+3)
val MetricRed = Color(0xFFFF1744)       // ROJO: Exceso importante de sílabas (>= +4)

// Rhyme Tag Palette for visual rhyme family coding (A, B, C, D...)
val RhymeColors = listOf(
    Color(0xFF00F5D4), // A - Cyan
    Color(0xFFC77DFF), // B - Violet
    Color(0xFFFF9E00), // C - Amber
    Color(0xFFFF2A85), // D - Pink
    Color(0xFF3A86FF), // E - Electric Blue
    Color(0xFF70E000), // F - Lime
    Color(0xFFFFBE0B), // G - Gold
    Color(0xFF00BBF9)  // H - Sky
)
