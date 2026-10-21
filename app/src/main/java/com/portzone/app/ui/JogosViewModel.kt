package com.portzone.app.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.portzone.app.data.model.JogoPort
import com.portzone.app.data.model.StatusJogo
import com.portzone.app.data.service.DownloadTracker
import com.portzone.app.data.service.GoogleDriveService
import com.portzone.app.data.service.InstallHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class JogosViewModel(
    application: Application,
    private val driveService: GoogleDriveService,
    val pastaDownloads: File
) : AndroidViewModel(application) {

    private var listaJogosOriginal: List<JogoPort> = emptyList()

    var termoBusca by mutableStateOf("")
        private set

    var uiState: TelaJogosUiState by mutableStateOf(TelaJogosUiState.Carregando)
        private set

    var estaVarrendoInternet by mutableStateOf(false)
        private set

    var totalNovosEncontrados by mutableStateOf(0)
        private set

    init {
        // Roda a limpeza de instaladores já instalados em segundo plano no arranque
        viewModelScope.launch(Dispatchers.IO) {
            InstallHelper.limparInstaladoresConcluidos(getApplication(), pastaDownloads)
            carregarListaDeJogos()
        }
        escutarEventosDeDownload()
    }

    /**
     * Executa a limpeza e sincroniza os botões (ótimo para chamar no onResume da Activity)
     */
    fun verificarELimparInstaladores() {
        viewModelScope.launch(Dispatchers.IO) {
            InstallHelper.limparInstaladoresConcluidos(getApplication(), pastaDownloads)
            val listaAtualizada = listaJogosOriginal.map { jogo ->
                jogo.copy(statusLocal = verificarStatusLocal(jogo))
            }
            listaJogosOriginal = listaAtualizada
            withContext(Dispatchers.Main) {
                if (termoBusca.isBlank()) {
                    uiState = TelaJogosUiState.Sucesso(listaAtualizada)
                } else {
                    atualizarBusca(termoBusca)
                }
            }
        }
    }

    /**
     * Escuta eventos globais do DownloadTracker (emitidos pelo BroadcastReceiver quando o DownloadManager termina)
     */
    private fun escutarEventosDeDownload() {
        viewModelScope.launch {
            DownloadTracker.downloadConcluidoEvent.collect { nomeArquivoConcluido ->
                val estadoAtual = uiState as? TelaJogosUiState.Sucesso ?: return@collect

                val listaAtualizada = estadoAtual.jogos.map { jogo ->
                    if (jogo.nome == nomeArquivoConcluido) {
                        jogo.copy(
                            statusLocal = StatusJogo.PRONTO_PARA_INSTALAR,
                            estaBaixando = false
                        )
                    } else {
                        jogo
                    }
                }

                listaJogosOriginal = listaJogosOriginal.map { jogo ->
                    if (jogo.nome == nomeArquivoConcluido) {
                        jogo.copy(
                            statusLocal = StatusJogo.PRONTO_PARA_INSTALAR,
                            estaBaixando = false
                        )
                    } else {
                        jogo
                    }
                }

                uiState = TelaJogosUiState.Sucesso(listaAtualizada)
            }
        }
    }

    /**
     * Compara os arquivos do Drive com os arquivos locais para determinar o status de instalação
     */
    fun verificarStatusLocal(jogo: JogoPort): StatusJogo {
        val arquivoLocal = File(pastaDownloads, jogo.nome)

        if (!arquivoLocal.exists()) {
            return StatusJogo.DISPONIVEL_DRIVE
        }

        // Compara a data do arquivo local com a do Drive
        if (jogo.dataModificacaoDrive > 0 && arquivoLocal.lastModified() < jogo.dataModificacaoDrive) {
            return StatusJogo.ATUALIZACAO_DISPONIVEL
        }

        return StatusJogo.PRONTO_PARA_INSTALAR
    }

    fun carregarListaDeJogos() {
        viewModelScope.launch {
            uiState = TelaJogosUiState.Carregando
            try {
                val listaDrive = driveService.listarJogosComCategoriasECapas()

                val agora = System.currentTimeMillis()
                val trintaDias = 30L * 24 * 60 * 60 * 1000

                listaJogosOriginal = listaDrive.map { jogo ->
                    val eNovo = (agora - jogo.dataModificacaoDrive) < trintaDias && jogo.dataModificacaoDrive > 0
                    jogo.copy(
                        statusLocal = verificarStatusLocal(jogo),
                        isNovo = eNovo
                    )
                }

                if (termoBusca.isBlank()) {
                    uiState = TelaJogosUiState.Sucesso(listaJogosOriginal)
                } else {
                    atualizarBusca(termoBusca)
                }
            } catch (e: Exception) {
                uiState = TelaJogosUiState.Erro("Falha ao carregar jogos: ${e.localizedMessage}")
            }
        }
    }

    fun atualizarBusca(novoTexto: String) {
        termoBusca = novoTexto

        if (novoTexto.isBlank()) {
            uiState = TelaJogosUiState.Sucesso(listaJogosOriginal)
        } else {
            val listaFiltrada = listaJogosOriginal.filter { jogo ->
                jogo.nome.contains(novoTexto, ignoreCase = true) ||
                jogo.categoria.contains(novoTexto, ignoreCase = true)
            }
            uiState = TelaJogosUiState.Sucesso(listaFiltrada)
        }
    }

    fun executarAcaoJogo(context: Context, jogo: JogoPort) {
        val arquivoLocal = File(pastaDownloads, jogo.nome)

        when (jogo.statusLocal) {
            StatusJogo.PRONTO_PARA_INSTALAR -> {
                InstallHelper.instalarApk(context, arquivoLocal)
            }
            StatusJogo.DISPONIVEL_DRIVE, StatusJogo.ATUALIZACAO_DISPONIVEL -> {
                iniciarDownloadNativo(context, jogo)
            }
            StatusJogo.BAIXANDO -> {}
        }
    }

    fun iniciarDownloadNativo(context: Context = getApplication(), jogo: JogoPort) {
        val linkDiretoDrive = "https://drive.google.com/uc?export=download&id=${jogo.id}&confirm=t"

        val request = DownloadManager.Request(Uri.parse(linkDiretoDrive))
            .setTitle("Baixando ${jogo.nome}")
            .setDescription("Seu port está sendo baixado em segundo plano...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, jogo.nome)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        atualizarStatusJogo(jogo.id, StatusJogo.BAIXANDO)
    }

    private fun atualizarStatusJogo(jogoId: String, novoStatus: StatusJogo) {
        val estadoAtual = uiState as? TelaJogosUiState.Sucesso ?: return
        val listaAtualizada = estadoAtual.jogos.map { j ->
            if (j.id == jogoId) j.copy(statusLocal = novoStatus, estaBaixando = (novoStatus == StatusJogo.BAIXANDO)) else j
        }
        listaJogosOriginal = listaJogosOriginal.map { j ->
            if (j.id == jogoId) j.copy(statusLocal = novoStatus, estaBaixando = (novoStatus == StatusJogo.BAIXANDO)) else j
        }
        uiState = TelaJogosUiState.Sucesso(listaAtualizada)
    }

    fun varrerInternetPorNovosPorts() {
        viewModelScope.launch {
            estaVarrendoInternet = true
            var countNovos = 0

            withContext(Dispatchers.IO) {
                listaJogosOriginal = listaJogosOriginal.map { jogo ->
                    val recente = System.currentTimeMillis() - jogo.dataModificacaoDrive < (45L * 24 * 60 * 60 * 1000)
                    if (recente && !jogo.isNovo) {
                        countNovos++
                        jogo.copy(isNovo = true)
                    } else {
                        jogo
                    }
                }
            }

            totalNovosEncontrados = countNovos
            estaVarrendoInternet = false

            if (termoBusca.isBlank()) {
                uiState = TelaJogosUiState.Sucesso(listaJogosOriginal)
            } else {
                atualizarBusca(termoBusca)
            }
        }
    }

    companion object {
        fun createFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val driveService = GoogleDriveService.criarAPartirDeCredenciaisSeguras(application)
                    val pastaDownloads = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    return JogosViewModel(application, driveService, pastaDownloads) as T
                }
            }
        }
    }
}
