package com.portzone.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portzone.app.data.model.JogoPort

@Composable
fun JogoItem(
    jogo: JogoPort,
    onDownloadClick: (JogoPort) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do Jogo
            Icon(
                imageVector = Icons.Default.Gamepad,
                contentDescription = "Ícone de Jogo",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Textos (Nome e Tamanho)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = jogo.nome,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                jogo.tamanho?.let { bytes ->
                    val tamanhoMB = bytes / (1024 * 1024)
                    Text(
                        text = "$tamanhoMB MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botão de Download ou Barra de Progresso
            if (jogo.estaBaixando) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    val porcentagem = (jogo.progresso * 100).toInt()
                    Text(
                        text = "$porcentagem%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { jogo.progresso },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else {
                IconButton(onClick = { onDownloadClick(jogo) }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Baixar Jogo",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
