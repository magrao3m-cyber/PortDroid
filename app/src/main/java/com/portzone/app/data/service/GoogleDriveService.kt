package com.portzone.app.data.service

import android.content.Context
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.portzone.app.data.model.JogoPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

class GoogleDriveService(private val driveService: Drive) {

    constructor(requestInitializer: HttpRequestInitializer) : this(
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            requestInitializer
        ).setApplicationName("PortZone Android").build()
    )

    /**
     * Estrutura a leitura das subpastas do Drive como Categorias (ex: "Ação", "Tiro", "Corrida")
     * e associa automaticamente arquivos de imagem (.jpg ou .png) com o mesmo nome do APK como capa.
     */
    suspend fun listarJogosComCategoriasECapas(idPastaRaiz: String = ID_PASTA_PADRAO): List<JogoPort> = withContext(Dispatchers.IO) {
        val todosOsJogos = mutableListOf<JogoPort>()

        try {
            // 1. Busca todas as subpastas dentro da pasta raiz
            val queryPastas = "'$idPastaRaiz' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val resultadoPastas = driveService.files().list()
                .setQ(queryPastas)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val subpastas = resultadoPastas.files ?: emptyList()

            // Se não houver subpastas, busca direto na raiz como categoria "Geral"
            if (subpastas.isEmpty()) {
                buscarJogosNaPasta(idPastaRaiz, "Geral", todosOsJogos)
            } else {
                // 2. Para cada subpasta (Categoria), busca os jogos e imagens dentro dela
                for (pasta in subpastas) {
                    val nomeCategoria = pasta.name ?: "Geral"
                    buscarJogosNaPasta(pasta.id, nomeCategoria, todosOsJogos)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: se falhar na busca com subpastas, tenta listar direto na pasta raiz
            try {
                buscarJogosNaPasta(idPastaRaiz, "Geral", todosOsJogos)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        return@withContext todosOsJogos
    }

    private fun buscarJogosNaPasta(pastaId: String, categoria: String, listaParaPreencher: MutableList<JogoPort>) {
        val queryArquivos = "'$pastaId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false"
        val resultadoArquivos = driveService.files().list()
            .setQ(queryArquivos)
            .setSpaces("drive")
            .setFields("files(id, name, size, modifiedTime, thumbnailLink, mimeType)")
            .execute()

        val arquivos = resultadoArquivos.files ?: return

        // Separa os APKs e arquivos de jogos das Imagens
        val apks = arquivos.filter {
            it.name.endsWith(".apk", ignoreCase = true) ||
            it.name.endsWith(".zip", ignoreCase = true) ||
            it.name.endsWith(".7z", ignoreCase = true) ||
            it.name.endsWith(".z64", ignoreCase = true)
        }
        val imagens = arquivos.filter {
            it.name.endsWith(".jpg", ignoreCase = true) ||
            it.name.endsWith(".png", ignoreCase = true) ||
            it.name.endsWith(".jpeg", ignoreCase = true)
        }

        for (apk in apks) {
            val nomeSemExtensao = apk.name.substringBeforeLast(".")

            // Busca se existe uma imagem com o mesmo nome na pasta para servir de capa
            val imagemCorrespondente = imagens.find {
                it.name.substringBeforeLast(".").equals(nomeSemExtensao, ignoreCase = true)
            }

            val urlCapa = imagemCorrespondente?.thumbnailLink ?: apk.thumbnailLink

            listaParaPreencher.add(
                JogoPort(
                    id = apk.id,
                    nome = apk.name,
                    tamanho = apk.getSize(),
                    dataModificacaoDrive = apk.modifiedTime?.value ?: 0L,
                    categoria = categoria,
                    urlCapa = urlCapa,
                    mimeType = apk.mimeType
                )
            )
        }
    }

    suspend fun listarJogos(idPasta: String = ID_PASTA_PADRAO): List<JogoPort> {
        return listarJogosComCategoriasECapas(idPasta)
    }

    /**
     * Download do arquivo para o caminho de destino com suporte a progresso (% de 0.0f a 1.0f).
     */
    suspend fun baixarJogo(
        fileId: String,
        caminhoDestino: String,
        onProgresso: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val arquivoDestino = File(caminhoDestino)
            arquivoDestino.parentFile?.mkdirs()

            val outputStream: OutputStream = FileOutputStream(arquivoDestino)
            val request = driveService.files().get(fileId)
            request.setAcknowledgeAbuse(true)

            if (onProgresso != null) {
                request.mediaHttpDownloader.progressListener =
                    MediaHttpDownloaderProgressListener { mediaDownloader ->
                        when (mediaDownloader.downloadState) {
                            MediaHttpDownloader.DownloadState.MEDIA_IN_PROGRESS -> {
                                onProgresso(mediaDownloader.progress.toFloat())
                            }
                            MediaHttpDownloader.DownloadState.MEDIA_COMPLETE -> {
                                onProgresso(1.0f)
                            }
                            else -> {}
                        }
                    }
            }

            request.executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    companion object {
        const val ID_PASTA_PADRAO = "1ZaaRjdB92s_JjgrzYIMnjRQ6Ns2yM7Fg"

        /**
         * Inicializa o GoogleDriveService de forma 100% segura utilizando o Android Keystore
         * e a biblioteca AndroidX Security Crypto (Tink).
         *
         * O JSON descriptografado é lido direto na memória RAM sem salvar texto puro no armazenamento.
         */
        fun criarAPartirDeCredenciaisSeguras(
            context: Context,
            nomeArquivoCriptografado: String = GerenciadorCredenciaisSeguras.NOME_ARQUIVO_PADRAO
        ): GoogleDriveService {
            var jsonCredenciais = GerenciadorCredenciaisSeguras.lerCredenciaisCriptografadas(
                context,
                nomeArquivoCriptografado
            )

            // Se o arquivo criptografado ainda não existe, verifica se há credentials.json em assets para migração inicial
            if (jsonCredenciais.isBlank()) {
                try {
                    val streamAssets = context.assets.open("credentials.json")
                    val jsonBruto = streamAssets.bufferedReader().use { it.readText() }
                    if (jsonBruto.isNotBlank()) {
                        // Criptografa imediatamente com chave do chip Keystore do aparelho
                        GerenciadorCredenciaisSeguras.salvarCredenciaisCriptografadas(
                            context,
                            jsonBruto,
                            nomeArquivoCriptografado
                        )
                        jsonCredenciais = jsonBruto
                    }
                } catch (_: Exception) {
                    // Esperado em builds de produção onde o arquivo de texto puro foi removido de assets
                }
            }

            if (jsonCredenciais.isBlank()) {
                throw IllegalStateException("Nenhuma credencial segura encontrada. Certifique-se de configurar o arquivo $nomeArquivoCriptografado.")
            }

            // Alimenta a biblioteca do Google Drive diretamente com o fluxo de memória seguro (RAM)
            val inputStream: InputStream = jsonCredenciais.byteInputStream()
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(Collections.singletonList(DriveScopes.DRIVE_READONLY))

            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val requestInitializer = HttpCredentialsAdapter(credentials)

            val drive = Drive.Builder(httpTransport, jsonFactory, requestInitializer)
                .setApplicationName("PortZone Android")
                .build()

            return GoogleDriveService(drive)
        }

        fun criarAPartirDeAssets(
            context: Context,
            nomeAsset: String = "credentials.json"
        ): GoogleDriveService {
            return try {
                criarAPartirDeCredenciaisSeguras(context)
            } catch (_: Exception) {
                val inputStream: InputStream = context.assets.open(nomeAsset)
                val credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singletonList(DriveScopes.DRIVE_READONLY))

                val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()
                val requestInitializer = HttpCredentialsAdapter(credentials)

                val drive = Drive.Builder(httpTransport, jsonFactory, requestInitializer)
                    .setApplicationName("PortZone Android")
                    .build()

                GoogleDriveService(drive)
            }
        }
    }
}
