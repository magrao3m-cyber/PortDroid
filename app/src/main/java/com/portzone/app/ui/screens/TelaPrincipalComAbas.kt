package com.portzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portzone.app.data.model.JogoPort
import com.portzone.app.ui.TelaJogosUiState
import com.portzone.app.ui.components.CardJogoPremium
import com.portzone.app.ui.components.DialogoGerenciadorDeDados
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipalComAbas(
    uiState: TelaJogosUiState,
    termoBusca: String,
    pastaDownloads: File,
    onBuscaAlterada: (String) -> Unit,
    onAcaoClick: (JogoPort) -> Unit,
    onRefreshClick: () -> Unit = {},
    onVarrerInternetClick: () -> Unit = {},
    estaVarrendo: Boolean = false,
    modifier: Modifier = Modifier
) {
    var abaSelecionada by remember { mutableStateOf(0) }
    var exibirDialogoDados by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF0B0C10))
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                // Header Minimalista Cyber-Gaming
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PORT STATION",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botão para gerenciar arquivos de dados e ROMs via SAF
                        IconButton(onClick = { exibirDialogoDados = true }) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = "Gerenciador de Dados e ROMs (SAF)",
                                tint = Color(0xFF00E5FF)
                            )
                        }

                        // Botão para varrer a internet em busca de novos ports
                        IconButton(
                            onClick = onVarrerInternetClick,
                            enabled = !estaVarrendo
                        ) {
                            if (estaVarrendo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00E5FF)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = "Buscar Novos Ports",
                                    tint = Color(0xFF00E5FF)
                                )
                            }
                        }

                        // Botão de atualizar a pasta do Drive
                        IconButton(onClick = onRefreshClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar Drive",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Campo de Busca Esportivo e Moderno
                OutlinedTextField(
                    value = termoBusca,
                    onValueChange = onBuscaAlterada,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = {
                        Text("Procurar entre seus ports...", color = Color(0xFF94A3B8))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF00E5FF)
                        )
                    },
                    trailingIcon = {
                        if (termoBusca.isNotEmpty()) {
                            IconButton(onClick = { onBuscaAlterada("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpar busca",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F2833),
                        unfocusedContainerColor = Color(0xFF1F2833),
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00E5FF)
                    )
                )

                // Abas Estilo Pílula Deslizante
                if (uiState is TelaJogosUiState.Sucesso) {
                    val categorias = remember(uiState.jogos) {
                        listOf("Todas") + uiState.jogos.map { it.categoria }.distinct()
                    }

                    ScrollableTabRow(
                        selectedTabIndex = abaSelecionada.coerceIn(0, (categorias.size - 1).coerceAtLeast(0)),
                        containerColor = Color.Transparent,
                        edgePadding = 16.dp,
                        indicator = {}, // Remove a linha tradicional embaixo da aba
                        divider = {}
                    ) {
                        categorias.forEachIndexed { index, categoria ->
                            val ativa = abaSelecionada == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (ativa) Color(0xFF00E5FF) else Color(0xFF1F2833))
                                    .clickable { abaSelecionada = index }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = categoria.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ativa) Color(0xFF0B0C10) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0B0C10) // Fundo gamer OLED profundo
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is TelaJogosUiState.Carregando -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Carregando catálogo de ports...",
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                is TelaJogosUiState.Erro -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.mensagem,
                            color = Color(0xFFFF5252),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefreshClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0B0C10)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar novamente", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is TelaJogosUiState.Sucesso -> {
                    val categorias = remember(uiState.jogos) {
                        listOf("Todas") + uiState.jogos.map { it.categoria }.distinct()
                    }
                    val categoriaAtiva = categorias.getOrElse(abaSelecionada) { "Todas" }

                    val jogosDaAba = if (categoriaAtiva == "Todas") {
                        uiState.jogos
                    } else {
                        uiState.jogos.filter { it.categoria == categoriaAtiva }
                    }

                    if (jogosDaAba.isEmpty()) {
                        Text(
                            text = if (termoBusca.isEmpty()) "Nenhum port nesta categoria." else "Nenhum resultado para '$termoBusca'",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
                        ) {
                            items(jogosDaAba, key = { it.id }) { jogo ->
                                CardJogoPremium(
                                    jogo = jogo,
                                    onAcaoClick = onAcaoClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (exibirDialogoDados) {
        DialogoGerenciadorDeDados(
            pastaDownloads = pastaDownloads,
            onDismiss = { exibirDialogoDados = false }
        )
    }
}
