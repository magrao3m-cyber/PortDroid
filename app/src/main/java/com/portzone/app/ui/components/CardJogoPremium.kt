package com.portzone.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.portzone.app.data.model.JogoPort
import com.portzone.app.data.model.StatusJogo

@Composable
fun CardJogoPremium(
    jogo: JogoPort,
    onAcaoClick: (JogoPort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F2833))
            .clickable { onAcaoClick(jogo) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail com cantos arredondados e borda sutil de profundidade
        Box {
            AsyncImage(
                model = jogo.urlCapa ?: "https://placehold.co/150x150/1F2833/00E5FF?text=${jogo.nome.take(2)}",
                contentDescription = "Capa de ${jogo.nome}",
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // Selo visual de jogo NOVO se detectado na varredura
            if (jogo.isNovo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            color = Color(0xFF00E5FF),
                            shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NOVO",
                        color = Color(0xFF0B0C10),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Dados do Jogo com Hierarquia Tipográfica
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = jogo.nome.substringBeforeLast("."), // Remove a extensão técnica visualmente
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val tamanhoMB = (jogo.tamanho ?: 0L) / (1024 * 1024)
            Text(
                text = if (tamanhoMB > 0) "$tamanhoMB MB" else "Tamanho sob consulta",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            if (jogo.statusLocal == StatusJogo.ATUALIZACAO_DISPONIVEL) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• ATUALIZAÇÃO RECOMENDADA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Botão Inteligente com Transição Dinâmica de Estados
        Box(
            modifier = Modifier.width(110.dp),
            contentAlignment = Alignment.Center
        ) {
            if (jogo.statusLocal == StatusJogo.BAIXANDO) {
                // Loader Circular Minimalista centralizado na área de ação
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF00E5FF),
                    strokeWidth = 2.5.dp
                )
            } else {
                val corBotaoAnimada by animateColorAsState(
                    targetValue = when (jogo.statusLocal) {
                        StatusJogo.PRONTO_PARA_INSTALAR -> Color(0xFF00E676)   // Verde Neon = Instalar
                        StatusJogo.ATUALIZACAO_DISPONIVEL -> Color(0xFFFF5252) // Vermelho Coral = Atualizar
                        else -> Color(0xFF00E5FF).copy(alpha = 0.15f)          // Ciano Opaco = Obter
                    },
                    label = "corBotaoJogo"
                )

                val corTexto = if (jogo.statusLocal == StatusJogo.PRONTO_PARA_INSTALAR) {
                    Color.Black
                } else {
                    Color.White
                }

                Button(
                    onClick = { onAcaoClick(jogo) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = corBotaoAnimada,
                        contentColor = corTexto
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (jogo.statusLocal) {
                            StatusJogo.DISPONIVEL_DRIVE -> "OBTER"
                            StatusJogo.PRONTO_PARA_INSTALAR -> "INSTALAR"
                            StatusJogo.ATUALIZACAO_DISPONIVEL -> "ATUALIZAR"
                            else -> "OBTER"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
