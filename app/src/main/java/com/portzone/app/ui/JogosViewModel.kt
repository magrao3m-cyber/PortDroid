package com.portzone.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
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
    private val driveService: GoogleDriveService?,
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

    fun obterIdPastaConfigurada(): String {
        val prefs = getApplication<Application>().getSharedPreferences("portzone_drive_prefs", Context.MODE_PRIVATE)
        return prefs.getString("custom_drive_folder_id", GoogleDriveService.ID_PASTA_PADRAO)
            ?: GoogleDriveService.ID_PASTA_PADRAO
    }

    fun salvarIdPastaDrive(novoIdOuLink: String) {
        val idExtraido = GoogleDriveService.extrairIdPasta(novoIdOuLink)
        val prefs = getApplication<Application>().getSharedPreferences("portzone_drive_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_drive_folder_id", idExtraido).apply()
        carregarListaDeJogos()
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
     * Escuta eventos globais do DownloadTracker
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
        val drive = driveService
        if (drive == null) {
            uiState = TelaJogosUiState.Erro("Google Drive não configurado. Verifique o arquivo de credenciais da Conta de Serviço.")
            return
        }

        viewModelScope.launch {
            uiState = TelaJogosUiState.Carregando
            try {
                val idPasta = obterIdPastaConfigurada()
                val listaDrive = drive.listarJogosComCategoriasECapas(idPasta)

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

        // Se for atalho de projeto (.url ou nome indicando link), abre diretamente no navegador
        if (jogo.nome.contains("(Link / Instruções)") || jogo.mimeType == "text/x-url" || jogo.nome.endsWith(".url", ignoreCase = true)) {
            abrirLinkProjetoOuArquivo(context, jogo)
            return
        }

        when (jogo.statusLocal) {
            StatusJogo.PRONTO_PARA_INSTALAR -> {
                InstallHelper.instalarApk(context, arquivoLocal)
            }
            StatusJogo.DISPONIVEL_DRIVE, StatusJogo.ATUALIZACAO_DISPONIVEL -> {
                iniciarDownloadAutenticado(context, jogo)
            }
            StatusJogo.BAIXANDO -> {}
        }
    }

    /**
     * Download autenticado direto pelo serviço Google Drive.
     * Funciona em pastas privadas de Contas de Serviço sem redirecionar para tela de login do Google.
     */
    fun iniciarDownloadAutenticado(context: Context = getApplication(), jogo: JogoPort) {
        val drive = driveService ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    atualizarStatusJogo(jogo.id, StatusJogo.BAIXANDO)
                }
                val arquivoDestino = File(pastaDownloads, jogo.nome)
                drive.baixarJogo(jogo.id, arquivoDestino.absolutePath)

                withContext(Dispatchers.Main) {
                    atualizarStatusJogo(jogo.id, StatusJogo.PRONTO_PARA_INSTALAR)
                    if (jogo.nome.endsWith(".apk", ignoreCase = true)) {
                        InstallHelper.instalarApk(context, arquivoDestino)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    atualizarStatusJogo(jogo.id, StatusJogo.DISPONIVEL_DRIVE)
                }
            }
        }
    }

    private fun abrirLinkProjetoOuArquivo(context: Context, jogo: JogoPort) {
        val drive = driveService
        if (drive == null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/open?id=${jogo.id}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "temp_${jogo.id}.url")
                drive.baixarJogo(jogo.id, tempFile.absolutePath)
                val content = tempFile.readText()
                tempFile.delete()

                val urlMatch = Regex("(?i)URL=(https?://[^\\r\\n]+)").find(content)
                val targetUrl = urlMatch?.groupValues?.get(1) ?: "https://drive.google.com/open?id=${jogo.id}"

                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/open?id=${jogo.id}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
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
                    val driveService = try {
                        GoogleDriveService.criarAPartirDeCredenciaisSeguras(application)
                    } catch (e1: Exception) {
                        try {
                            GoogleDriveService.criarAPartirDeAssets(application)
                        } catch (e2: Exception) {
                            null
                        }
                    }
                    val pastaDownloads = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    return JogosViewModel(application, driveService, pastaDownloads) as T
                }
            }
        }
    }
}
