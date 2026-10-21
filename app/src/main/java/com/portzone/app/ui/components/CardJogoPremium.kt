package com.portzone.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.portzone.app.data.model.JogoPort
import com.portzone.app.data.model.StatusJogo
import com.portzone.app.data.service.CoverArtResolver

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
        // Thumbnail com cantos arredondados, gradiente e suporte a fallback dinâmico
        var falhaNoCarregamento by remember(jogo.id, jogo.urlCapa) { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF161E28), Color(0xFF0F141C))
                    )
                )
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Tenta carregar a imagem de capa (seja do Drive ou resolvida pelo CoverArtResolver)
            if (!jogo.urlCapa.isNullOrBlank() && !falhaNoCarregamento) {
                AsyncImage(
                    model = jogo.urlCapa,
                    contentDescription = "Capa de ${jogo.nome}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onError = { falhaNoCarregamento = true }
                )
            }

            // Fallback Gamer Estilizado: Badge Cyber com ícone e iniciais do jogo
            if (jogo.urlCapa.isNullOrBlank() || falhaNoCarregamento) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val iniciais = CoverArtResolver.extrairIniciais(jogo.nome)
                    Text(
                        text = iniciais,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }

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
            val tituloExibicao = CoverArtResolver.normalizarNome(jogo.nome)
            Text(
                text = tituloExibicao,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val tamanhoMB = (jogo.tamanho ?: 0L) / (1024 * 1024)
            val subTexto = if (tamanhoMB > 0) {
                "$tamanhoMB MB • ${jogo.categoria}"
            } else {
                jogo.categoria
            }

            Text(
                text = subTexto,
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
                            StatusJogo.DISPONIVEL_DRIVE -> if (jogo.nome.contains("(Link")) "ABRIR" else "OBTER"
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
