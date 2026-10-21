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
     * Estrutura a leitura das subpastas do Drive como Categorias e Jogos em modo BULK de alta performance.
     * Elimina centenas de requisições HTTP individuais, carregando o catálogo completo em poucos segundos.
     */
    suspend fun listarJogosComCategoriasECapas(idPastaRaiz: String = ID_PASTA_PADRAO): List<JogoPort> = withContext(Dispatchers.IO) {
        val idLimpo = extrairIdPasta(idPastaRaiz)
        val todosOsJogos = mutableListOf<JogoPort>()

        try {
            // 1. Busca todas as subpastas dentro da pasta raiz com suporte a paginação
            val pastasMap = mutableMapOf<String, String>() // id -> nome
            var pageTokenPastas: String? = null
            do {
                val reqPastas = driveService.files().list()
                    .setQ("'$idLimpo' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageSize(1000)
                if (!pageTokenPastas.isNullOrEmpty()) {
                    reqPastas.pageToken = pageTokenPastas
                }
                val resultadoPastas = reqPastas.execute()
                resultadoPastas.files?.forEach { pasta ->
                    if (pasta.id != null && pasta.name != null) {
                        pastasMap[pasta.id] = pasta.name
                    }
                }
                pageTokenPastas = resultadoPastas.nextPageToken
            } while (!pageTokenPastas.isNullOrEmpty())

            // 2. Busca todos os arquivos que não são pastas de forma BULK (em lote)
            // Em vez de 968 requisições lentas, faz apenas 2 a 3 chamadas paginadas de 1000 itens
            val arquivosPorPasta = mutableMapOf<String, MutableList<com.google.api.services.drive.model.File>>()
            val arquivosRaiz = mutableListOf<com.google.api.services.drive.model.File>()
            var pageTokenArquivos: String? = null

            do {
                val reqArquivos = driveService.files().list()
                    .setQ("trashed = false and mimeType != 'application/vnd.google-apps.folder'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, size, modifiedTime, thumbnailLink, mimeType, parents)")
                    .setPageSize(1000)
                if (!pageTokenArquivos.isNullOrEmpty()) {
                    reqArquivos.pageToken = pageTokenArquivos
                }
                val resultadoArquivos = reqArquivos.execute()
                resultadoArquivos.files?.forEach { file ->
                    val parents = file.parents ?: emptyList()
                    if (parents.contains(idLimpo)) {
                        arquivosRaiz.add(file)
                    }
                    for (p in parents) {
                        if (pastasMap.containsKey(p)) {
                            arquivosPorPasta.getOrPut(p) { mutableListOf() }.add(file)
                        }
                    }
                }
                pageTokenArquivos = resultadoArquivos.nextPageToken
            } while (!pageTokenArquivos.isNullOrEmpty())

            // 3. Montar os itens de JogoPort cruzando pastas e arquivos em memória RAM:
            for ((pastaId, nomePasta) in pastasMap) {
                val arquivos = arquivosPorPasta[pastaId] ?: emptyList()

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

                val nomeLimpo = nomePasta.replace(Regex("\\s*\\(\\d+\\)$"), "").trim()
                val categoria = extrairCategoria(nomeLimpo)

                if (apks.isNotEmpty()) {
                    for (apk in apks) {
                        val nomeSemExtensao = apk.name.substringBeforeLast(".")
                        val imagemCorrespondente = imagens.find {
                            it.name.substringBeforeLast(".").equals(nomeSemExtensao, ignoreCase = true)
                        }
                        val urlCapa = imagemCorrespondente?.thumbnailLink ?: apk.thumbnailLink

                        todosOsJogos.add(
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
                } else {
                    // Se não houver APK direto, mapeia atalhos de projetos (.url) ou arquivos de instruções (.txt)
                    val urlFile = arquivos.find { it.name.endsWith(".url", ignoreCase = true) }
                    val txtFile = arquivos.find { it.name.endsWith(".txt", ignoreCase = true) }
                    val primeiro = urlFile ?: txtFile ?: arquivos.firstOrNull()

                    if (primeiro != null) {
                        todosOsJogos.add(
                            JogoPort(
                                id = primeiro.id,
                                nome = "$nomeLimpo (Link / Instruções)",
                                tamanho = primeiro.getSize(),
                                dataModificacaoDrive = primeiro.modifiedTime?.value ?: 0L,
                                categoria = categoria,
                                urlCapa = imagens.firstOrNull()?.thumbnailLink,
                                mimeType = primeiro.mimeType
                            )
                        )
                    }
                }
            }

            // 4. Arquivos diretamente na raiz da pasta
            for (apk in arquivosRaiz) {
                if (apk.name.endsWith(".apk", ignoreCase = true) ||
                    apk.name.endsWith(".zip", ignoreCase = true) ||
                    apk.name.endsWith(".7z", ignoreCase = true)) {
                    todosOsJogos.add(
                        JogoPort(
                            id = apk.id,
                            nome = apk.name,
                            tamanho = apk.getSize(),
                            dataModificacaoDrive = apk.modifiedTime?.value ?: 0L,
                            categoria = "Geral",
                            urlCapa = apk.thumbnailLink,
                            mimeType = apk.mimeType
                        )
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        todosOsJogos.sortBy { it.nome }
        return@withContext todosOsJogos
    }

    private fun extrairCategoria(nomePasta: String): String {
        val lower = nomePasta.lowercase()
        return when {
            lower.contains("touch") -> "Touch Ports"
            lower.contains("source engine") || lower.contains("xash") -> "Source / Valve"
            lower.contains("recomp") -> "PC Recomp"
            lower.contains("xcom") || lower.contains("openxcom") -> "Estratégia"
            lower.contains("mario") || lower.contains("zelda") || lower.contains("sonic") || lower.contains("star fox") -> "Clássicos"
            lower.contains("doom") || lower.contains("quake") || lower.contains("wolfenstein") || lower.contains("duke") -> "FPS Retrô"
            lower.contains("rpg") || lower.contains("morrowind") || lower.contains("undertale") -> "RPG / Aventura"
            nomePasta.contains(" - ") -> {
                val partes = nomePasta.split(" - ")
                if (partes.size > 1 && partes[1].isNotBlank()) partes[1].trim() else "Geral"
            }
            else -> "Geral"
        }
    }

    suspend fun listarJogos(idPasta: String = ID_PASTA_PADRAO): List<JogoPort> {
        return listarJogosComCategoriasECapas(idPasta)
    }

    /**
     * Download autenticado do arquivo para o caminho de destino com suporte a progresso (% de 0.0f a 1.0f).
     * Utiliza as credenciais da Conta de Serviço para permitir downloads de pastas privadas sem redirecionamentos de login.
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
         * Extrai o ID da pasta caso o usuário cole um link completo do Google Drive.
         */
        fun extrairIdPasta(linkOuId: String): String {
            val trimmed = linkOuId.trim()
            val regex = Regex("folders/([a-zA-Z0-9_-]+)")
            val match = regex.find(trimmed)
            return match?.groupValues?.get(1) ?: trimmed
        }

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
