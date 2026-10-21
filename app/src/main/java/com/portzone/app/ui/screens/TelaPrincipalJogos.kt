package com.portzone.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portzone.app.data.model.JogoPort
import com.portzone.app.ui.TelaJogosUiState
import com.portzone.app.ui.components.JogoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipalJogos(
    uiState: TelaJogosUiState,
    termoBusca: String,
    onBuscaAlterada: (String) -> Unit,
    onDownloadClick: (JogoPort) -> Unit,
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    // Título do App com botão de atualização
                    TopAppBar(
                        title = { Text("Meus Ports de Jogos") },
                        actions = {
                            IconButton(onClick = onRefreshClick) {
                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    // Campo de Pesquisa
                    OutlinedTextField(
                        value = termoBusca,
                        onValueChange = onBuscaAlterada,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("Pesquisar jogo...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                        },
                        trailingIcon = {
                            if (termoBusca.isNotEmpty()) {
                                IconButton(onClick = { onBuscaAlterada("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
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
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Buscando jogos no seu Google Drive...")
                    }
                }
                is TelaJogosUiState.Erro -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.mensagem,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRefreshClick) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tentar novamente")
                        }
                    }
                }
                is TelaJogosUiState.Sucesso -> {
                    if (uiState.jogos.isEmpty()) {
                        Text(
                            text = if (termoBusca.isEmpty()) "Nenhum jogo encontrado na pasta." else "Nenhum resultado para '$termoBusca'",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.jogos, key = { it.id }) { jogo ->
                                JogoItem(
                                    jogo = jogo,
                                    onDownloadClick = onDownloadClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
