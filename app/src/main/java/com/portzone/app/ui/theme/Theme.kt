package com.portzone.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta Cyber-Minimalism para Ecossistema Mobile/Gaming
val CoresGamerEscuro = darkColorScheme(
    primary = Color(0xFF00E676),          // Verde Neon vibrante para "Baixar" e "Instalar"
    primaryContainer = Color(0xFF0B0C10), // Fundo ultra escuro gamer
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF00E5FF),        // Ciano elétrico para progresso e abas ativas
    surface = Color(0xFF0B0C10),          // Fundo principal OLED profundo
    surfaceVariant = Color(0xFF1F2833),   // Grafite fosco dos cards (ilusão de profundidade)
    onSurface = Color(0xFFFFFFFF),        // Branco puro para títulos principais
    onSurfaceVariant = Color(0xFF94A3B8), // Cinza azulado para subtítulos e dados técnicos
    error = Color(0xFFFF5252)             // Vermelho coral para alertas de atualização
)

@Composable
fun AppPortsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoresGamerEscuro,
        content = content
    )
}

// Alias para compatibilidade
@Composable
fun PortZoneTheme(content: @Composable () -> Unit) {
    AppPortsTheme(content = content)
}
