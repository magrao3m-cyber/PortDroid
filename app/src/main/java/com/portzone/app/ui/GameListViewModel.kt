package com.portzone.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.portzone.app.data.model.DownloadStatus
import com.portzone.app.data.model.JogoPort
import com.portzone.app.data.model.PortGame
import com.portzone.app.data.repository.GameRepository
import com.portzone.app.data.service.DownloadHelper
import com.portzone.app.data.service.GoogleDriveService
import com.portzone.app.data.service.SheetCatalogService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CatalogUiState {
    object Loading : CatalogUiState
    data class Success(val games: List<PortGame>) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}

class GameListViewModel(application: Application) : AndroidViewModel(application) {

    val pastaDriveIdPadrao = "1ZaaRjdB92s_JjgrzYIMnjRQ6Ns2yM7Fg"

    // Estado da Tela 1: Jogos do Meu Google Drive
    private val _driveUiState = MutableStateFlow<TelaJogosUiState>(TelaJogosUiState.Carregando)
    val driveUiState: StateFlow<TelaJogosUiState> = _driveUiState.asStateFlow()

    // Estado da Tela 2: Catálogo da Comunidade (485+ Jogos)
    private val _rawGames = MutableStateFlow<List<PortGame>>(emptyList())
    private val _isLoadingCatalog = MutableStateFlow(true)
    private val _catalogError = MutableStateFlow<String?>(null)

    val searchQuery = MutableStateFlow("")
    val filterOnlyWithRoms = MutableStateFlow(false)

    private val _downloadsState = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadsState: StateFlow<Map<String, DownloadStatus>> = _downloadsState.asStateFlow()

    private val repository: GameRepository
    private val driveService: GoogleDriveService?

    val catalogUiState: StateFlow<CatalogUiState> = combine(
        _rawGames,
        _isLoadingCatalog,
        _catalogError,
        searchQuery,
        filterOnlyWithRoms
    ) { games, loading, error, query, onlyRoms ->
        when {
            loading -> CatalogUiState.Loading
            error != null -> CatalogUiState.Error(error)
            else -> {
                val filtrados = games.filter { game ->
                    val matchesQuery = query.isBlank() ||
                            game.name.contains(query, ignoreCase = true) ||
                            game.project.contains(query, ignoreCase = true) ||
                            game.needsGameFiles.contains(query, ignoreCase = true)

                    val matchesRom = !onlyRoms || game.hasRomRequirement
                    matchesQuery && matchesRom
                }
                CatalogUiState.Success(filtrados)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CatalogUiState.Loading)

    init {
        val context = application.applicationContext
        val sheetService = SheetCatalogService(context)
        val downloadHelper = DownloadHelper(context)

        var service: GoogleDriveService? = null
        try {
            service = GoogleDriveService.criarAPartirDeCredenciaisSeguras(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        driveService = service

        repository = GameRepository(sheetService, driveService, downloadHelper, context)

        // Carrega o Google Drive e o Catálogo inicial
        carregarJogosDrive()
        carregarCatalogoComunidade()
    }

    /**
     * Busca os arquivos dentro da pasta PORTS do Google Drive
     */
    fun carregarJogosDrive(idPasta: String = pastaDriveIdPadrao) {
        val drive = driveService
        if (drive == null) {
            _driveUiState.value = TelaJogosUiState.Erro("credentials.json não encontrado em assets/")
            return
        }

        viewModelScope.launch {
            _driveUiState.value = TelaJogosUiState.Carregando
            try {
                val lista = drive.listarJogos(idPasta)
                _driveUiState.value = TelaJogosUiState.Sucesso(lista)
            } catch (erro: Exception) {
                _driveUiState.value = TelaJogosUiState.Erro(
                    erro.localizedMessage ?: "Erro ao buscar arquivos no Google Drive."
                )
            }
        }
    }

    /**
     * Download de um jogo da lista do Google Drive
     */
    fun baixarJogoDrive(jogo: JogoPort) {
        try {
            _downloadsState.update { it + (jogo.id to DownloadStatus.Downloading(0)) }
            val downloadId = repository.baixarArquivoDrive(jogo.id, jogo.nome)
            _downloadsState.update { it + (jogo.id to DownloadStatus.Enqueued(downloadId)) }
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadsState.update { it + (jogo.id to DownloadStatus.Error(e.message ?: "Erro ao iniciar download")) }
        }
    }

    /**
     * Carrega os 485 jogos da planilha da comunidade
     */
    fun carregarCatalogoComunidade(forcarRecarregar: Boolean = false) {
        viewModelScope.launch {
            _isLoadingCatalog.value = true
            _catalogError.value = null

            val resultado = repository.obterCatalogo(forcarLocal = false)
            resultado.onSuccess { lista ->
                _rawGames.value = lista
                _isLoadingCatalog.value = false
            }.onFailure { erro ->
                _catalogError.value = "Erro ao carregar lista: ${erro.localizedMessage ?: erro.message}"
                _isLoadingCatalog.value = false
            }
        }
    }

    fun abrirProjeto(game: PortGame) {
        repository.abrirLinkProjeto(game.projectUrl)
    }
}
