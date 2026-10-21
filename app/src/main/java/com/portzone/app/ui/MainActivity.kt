package com.portzone.app.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.portzone.app.data.model.PortGame
import com.portzone.app.ui.screens.TelaPrincipalComAbas
import com.portzone.app.ui.theme.AppPortsTheme

class MainActivity : ComponentActivity() {

    private val jogosViewModel: JogosViewModel by viewModels {
        JogosViewModel.createFactory(application)
    }

    private val catalogViewModel: GameListViewModel by viewModels()

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            jogosViewModel.carregarListaDeJogos()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadCompleteReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadCompleteReceiver, filter)
        }

        setContent {
            AppPortsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    var hasNotificationPermission by remember {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            } else true
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasNotificationPermission = isGranted
                    }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val catalogUiState by catalogViewModel.catalogUiState.collectAsState()
                    val catalogSearchQuery by catalogViewModel.searchQuery.collectAsState()
                    val onlyRoms by catalogViewModel.filterOnlyWithRoms.collectAsState()

                    var abaSelecionada by remember { mutableStateOf(0) }

                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                NavigationBarItem(
                                    selected = abaSelecionada == 0,
                                    onClick = { abaSelecionada = 0 },
                                    label = { Text("Meu Drive") },
                                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = abaSelecionada == 1,
                                    onClick = { abaSelecionada = 1 },
                                    label = { Text("Explorar (485+)") },
                                    icon = { Icon(Icons.Default.Gamepad, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (abaSelecionada) {
                                0 -> TelaPrincipalComAbas(
                                    uiState = jogosViewModel.uiState,
                                    termoBusca = jogosViewModel.termoBusca,
                                    pastaDownloads = jogosViewModel.pastaDownloads,
                                    idPastaAtual = jogosViewModel.obterIdPastaConfigurada(),
                                    onBuscaAlterada = { novoTexto -> jogosViewModel.atualizarBusca(novoTexto) },
                                    onAcaoClick = { jogo -> jogosViewModel.executarAcaoJogo(this@MainActivity, jogo) },
                                    onRefreshClick = { jogosViewModel.carregarListaDeJogos() },
                                    onVarrerInternetClick = { jogosViewModel.varrerInternetPorNovosPorts() },
                                    onSalvarIdPasta = { novaPasta -> jogosViewModel.salvarIdPastaDrive(novaPasta) },
                                    estaVarrendo = jogosViewModel.estaVarrendoInternet
                                )
                                1 -> CatalogScreen(
                                    uiState = catalogUiState,
                                    searchQuery = catalogSearchQuery,
                                    onlyRoms = onlyRoms,
                                    onSearchChange = { catalogViewModel.searchQuery.value = it },
                                    onToggleOnlyRoms = { catalogViewModel.filterOnlyWithRoms.value = !onlyRoms },
                                    onOpenProject = { catalogViewModel.abrirProjeto(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Assim que o usuário volta da tela do instalador do Android,
        // verifica se o jogo já foi instalado e remove o instalador duplicado da pasta Downloads
        jogosViewModel.verificarELimparInstaladores()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(downloadCompleteReceiver)
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    uiState: CatalogUiState,
    searchQuery: String,
    onlyRoms: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleOnlyRoms: () -> Unit,
    onOpenProject: (PortGame) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar no catálogo (ex: Mario, Zelda)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        FilterChip(
            selected = onlyRoms,
            onClick = onToggleOnlyRoms,
            label = { Text("Requer ROM/Arquivos", fontSize = 12.sp) },
            leadingIcon = {
                if (onlyRoms) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState) {
            is CatalogUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            is CatalogUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CatalogUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.games, key = { it.id }) { game ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = game.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Port: ${game.project}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                if (game.needsGameFiles.isNotBlank() && !game.needsGameFiles.equals("NO", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Requer: ${game.needsGameFiles}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { onOpenProject(game) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = androidx.compose.ui.graphics.Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (game.isGitHub) Icons.Default.Code else Icons.Default.Launch,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (game.isGitHub) "Ver no GitHub" else "Acessar Projeto",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
