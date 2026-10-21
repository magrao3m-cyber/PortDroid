package com.portzone.app.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class DestinoPortInfo(
    val nomeJogo: String,
    val pastaSugerida: String,
    val descricaoArquivos: String
)

data class ComplementaryFileInfo(
    val arquivo: File,
    val extensao: String,
    val tamanhoFormatado: String,
    val destinoSugerido: DestinoPortInfo
)

/**
 * Gerenciador de Arquivos de Dados e ROMs via Storage Access Framework (SAF).
 * Permite copiar ou extrair .wad, .pk3, .pak, .z64, .zip e outros formatos
 * diretamente nas pastas dos ports e emuladores do Android sem sofrer bloqueios do Scoped Storage.
 */
object SafGameDataManager {

    private const val PREFS_SAF = "portzone_saf_prefs"

    // Extensões reconhecidas de ROMs, WADs, PAKs e dados complementares de ports
    val EXTENSOES_COMPLEMENTARES = setOf(
        "wad", "pk3", "pak", "grp", "z64", "n64", "v64",
        "zip", "7z", "iso", "bin", "cue", "mpq", "dat",
        "bsa", "esm", "esp", "def", "hog"
    )

    /**
     * Mapeia regras para sugerir a pasta padrão do jogo baseado no nome do arquivo
     */
    fun sugerirDestino(nomeArquivo: String): DestinoPortInfo {
        val lower = nomeArquivo.lowercase()
        return when {
            lower.contains("doom") || lower.endsWith(".wad") ->
                DestinoPortInfo("Doom Engine (PrBoom / Delta / GZDoom)", "doom", "WADs de Doom (DOOM.WAD, DOOM2.WAD)")
            lower.contains("quake") || lower.endsWith(".pak") ->
                DestinoPortInfo("Quake Engine (Quake 1 / 2 / 3)", "id1", "PAKs e mapas (pak0.pak, baseq2, baseq3)")
            lower.contains("half") || lower.contains("xash") || lower.contains("valve") || lower.contains("cstrike") ->
                DestinoPortInfo("Half-Life / CS 1.6 (Xash3D)", "xash", "Diretório de dados valve/ ou cstrike/")
            lower.contains("duke") || lower.endsWith(".grp") ->
                DestinoPortInfo("Duke Nukem 3D (EDuke32)", "duke3d", "Arquivo DUKE3D.GRP principal")
            lower.contains("mario") || lower.contains("sm64") || lower.endsWith(".z64") ->
                DestinoPortInfo("Super Mario 64 Android Port", "sm64", "ROM original baserom.us.z64 ou assets")
            lower.contains("diablo") || lower.endsWith(".mpq") ->
                DestinoPortInfo("Diablo 1 (DevilutionX)", "devilutionx", "Arquivo DIABDAT.MPQ")
            lower.contains("fallout") ->
                DestinoPortInfo("Fallout 1 / 2 (fo2.exe)", "fo2.exe", "Arquivos master.dat e critter.dat")
            lower.contains("morrowind") || lower.endsWith(".esm") || lower.endsWith(".bsa") ->
                DestinoPortInfo("The Elder Scrolls III: Morrowind (OpenMW)", "openmw/data", "Data Files/ do Morrowind")
            lower.contains("rtcw") || lower.contains("wolf") ->
                DestinoPortInfo("Return to Castle Wolfenstein (RTCW Touch)", "rtcw/main", "Arquivos pak0.pk3 do jogo")
            lower.contains("gta") ->
                DestinoPortInfo("Grand Theft Auto Port", "data", "Arquivos de áudio e texturas do GTA")
            else ->
                DestinoPortInfo("Port / Emulador", "ports_data", "Arquivos de dados complementares do jogo")
        }
    }

    /**
     * Lista todos os arquivos complementares (não-APKs) presentes na pasta de downloads do app
     */
    fun listarArquivosComplementaresLocais(pastaDownloads: File): List<ComplementaryFileInfo> {
        if (!pastaDownloads.exists() || !pastaDownloads.isDirectory) return emptyList()

        val arquivos = pastaDownloads.listFiles() ?: return emptyList()
        val resultado = mutableListOf<ComplementaryFileInfo>()

        for (arq in arquivos) {
            if (arq.isFile) {
                val ext = arq.extension.lowercase()
                if (EXTENSOES_COMPLEMENTARES.contains(ext) && !arq.name.endsWith(".apk", ignoreCase = true)) {
                    val tamanhoMB = arq.length() / (1024 * 1024)
                    val tamanhoStr = if (tamanhoMB > 0) "$tamanhoMB MB" else "${arq.length() / 1024} KB"
                    resultado.add(
                        ComplementaryFileInfo(
                            arquivo = arq,
                            extensao = ext,
                            tamanhoFormatado = tamanhoStr,
                            destinoSugerido = sugerirDestino(arq.name)
                        )
                    )
                }
            }
        }
        return resultado
    }

    /**
     * Copia um arquivo físico local para uma pasta selecionada via Storage Access Framework (SAF)
     */
    suspend fun copiarArquivoParaSafTree(
        context: Context,
        treeUri: Uri,
        arquivoOrigem: File,
        onProgresso: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            if (!rootDoc.canWrite()) return@withContext false

            // Se for arquivo zip, podemos extrair diretamente dentro da pasta SAF
            if (arquivoOrigem.name.endsWith(".zip", ignoreCase = true)) {
                return@withContext extrairZipParaSafTree(context, rootDoc, arquivoOrigem, onProgresso)
            }

            // Remove arquivo de mesmo nome se já existir para substituição limpa
            val existente = rootDoc.findFile(arquivoOrigem.name)
            existente?.delete()

            val mimeType = quandoMimeType(arquivoOrigem.extension)
            val novoDoc = rootDoc.createFile(mimeType, arquivoOrigem.name) ?: return@withContext false

            val totalBytes = arquivoOrigem.length().coerceAtLeast(1L)
            var bytesCopiados = 0L

            val inputStream: InputStream = FileInputStream(arquivoOrigem)
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(novoDoc.uri)

            if (outputStream == null) {
                inputStream.close()
                return@withContext false
            }

            val buffer = ByteArray(32 * 1024)
            var bytesLidos: Int
            while (inputStream.read(buffer).also { bytesLidos = it } != -1) {
                outputStream.write(buffer, 0, bytesLidos)
                bytesCopiados += bytesLidos
                onProgresso((bytesCopiados.toFloat() / totalBytes).coerceIn(0f, 1f))
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            onProgresso(1.0f)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Extrai um arquivo .ZIP diretamente dentro da pasta DocumentFile acessada pelo SAF
     */
    private fun extrairZipParaSafTree(
        context: Context,
        rootDoc: DocumentFile,
        arquivoZip: File,
        onProgresso: (Float) -> Unit
    ): Boolean {
        return try {
            val totalBytes = arquivoZip.length().coerceAtLeast(1L)
            var bytesProcessados = 0L

            ZipInputStream(FileInputStream(arquivoZip)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val nomeArquivo = entry.name.substringAfterLast("/")
                        val subPastaNome = if (entry.name.contains("/")) entry.name.substringBeforeLast("/") else null

                        var pastaDestino = rootDoc
                        if (subPastaNome != null) {
                            val subPastas = subPastaNome.split("/").filter { it.isNotBlank() }
                            for (sub in subPastas) {
                                val existente = pastaDestino.findFile(sub)
                                pastaDestino = if (existente != null && existente.isDirectory) {
                                    existente
                                } else {
                                    pastaDestino.createDirectory(sub) ?: pastaDestino
                                }
                            }
                        }

                        val arquivoExistente = pastaDestino.findFile(nomeArquivo)
                        arquivoExistente?.delete()

                        val mimeType = quandoMimeType(nomeArquivo.substringAfterLast(".", ""))
                        val novoArquivoDoc = pastaDestino.createFile(mimeType, nomeArquivo)

                        if (novoArquivoDoc != null) {
                            context.contentResolver.openOutputStream(novoArquivoDoc.uri)?.use { os ->
                                val buffer = ByteArray(16 * 1024)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    os.write(buffer, 0, len)
                                    bytesProcessados += len
                                    onProgresso((bytesProcessados.toFloat() / totalBytes).coerceIn(0f, 1f))
                                }
                                os.flush()
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            onProgresso(1.0f)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun salvarUriPersistente(context: Context, treeUri: Uri, chaveJogo: String = "default") {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)

            val prefs = context.getSharedPreferences(PREFS_SAF, Context.MODE_PRIVATE)
            prefs.edit().putString("tree_uri_$chaveJogo", treeUri.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun obterUriSalva(context: Context, chaveJogo: String = "default"): Uri? {
        val prefs = context.getSharedPreferences(PREFS_SAF, Context.MODE_PRIVATE)
        val uriStr = prefs.getString("tree_uri_$chaveJogo", null) ?: return null
        val uri = Uri.parse(uriStr)

        val granted = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        return if (granted) uri else null
    }

    private fun quandoMimeType(extensao: String): String {
        return when (extensao.lowercase()) {
            "wad", "pk3", "pak", "grp", "mpq", "dat", "bsa" -> "application/octet-stream"
            "z64", "n64", "v64" -> "application/x-n64-rom"
            "zip" -> "application/zip"
            "7z" -> "application/x-7z-compressed"
            "iso" -> "application/x-iso9660-image"
            else -> "application/octet-stream"
        }
    }
}
