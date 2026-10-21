package com.portzone.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.portzone.app.data.model.PortGame
import com.portzone.app.data.service.DownloadHelper
import com.portzone.app.data.service.GoogleDriveService
import com.portzone.app.data.service.SheetCatalogService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(
    private val sheetService: SheetCatalogService,
    private val driveService: GoogleDriveService?,
    private val downloadHelper: DownloadHelper,
    private val context: Context
) {

    /**
     * Obtém a lista completa de jogos a partir do Google Sheets (com fallback offline).
     */
    suspend fun obterCatalogo(forcarLocal: Boolean = false): Result<List<PortGame>> {
        return sheetService.carregarCatalogo(forcarLocal)
    }

    /**
     * Sincroniza o catálogo do Google Sheets com uma pasta do Google Drive do usuário.
     * Vincula automaticamente os arquivos da pasta do Drive aos jogos correspondentes.
     */
    suspend fun sincronizarComDrive(
        jogos: List<PortGame>,
        idPastaDrive: String
    ): Result<List<PortGame>> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext Result.failure(
            IllegalStateException("Google Drive Service não configurado ou sem credentials.json")
        )

        try {
            val arquivosDrive = drive.listarJogos(idPastaDrive)
            val mapaDrive = arquivosDrive.associateBy { it.nome.lowercase().trim() }

            val jogosSincronizados = jogos.map { jogo ->
                val nomeJogoLimpo = jogo.name.lowercase().trim()
                val nomeArquivoEsperado = jogo.needsGameFiles.lowercase().trim()

                val arquivoCorrespondente = mapaDrive.entries.firstOrNull { (nomeDrive, _) ->
                    nomeDrive.contains(nomeJogoLimpo) ||
                            (nomeArquivoEsperado.isNotBlank() && nomeDrive.contains(nomeArquivoEsperado))
                }?.value

                if (arquivoCorrespondente != null) {
                    jogo.copy(
                        driveFileId = arquivoCorrespondente.id,
                        driveFileSize = arquivoCorrespondente.tamanho
                    )
                } else {
                    jogo
                }
            }
            Result.success(jogosSincronizados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia o download de um arquivo do Google Drive associado a um jogo.
     */
    fun baixarArquivoDrive(fileId: String, fileName: String): Long {
        return downloadHelper.enfileirarDownloadDrive(fileId, fileName)
    }

    /**
     * Abre a URL do projeto (GitHub Releases, Play Store, etc.) no navegador ou loja oficial.
     */
    fun abrirLinkProjeto(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
