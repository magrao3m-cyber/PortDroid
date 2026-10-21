package com.portzone.app.data.service

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

class DownloadHelper(private val context: Context) {

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * Enfileira o download no DownloadManager do Android.
     * Ideal para arquivos grandes (jogos/APKs/ZIPs), pois o sistema operacional gerencia
     * o download em segundo plano com barra de notificação, pausa e retentativa automática.
     *
     * @param fileId ID do arquivo no Google Drive.
     * @param fileName Nome com o qual o arquivo será salvo.
     * @return ID do download retornado pelo DownloadManager.
     */
    fun enfileirarDownloadDrive(fileId: String, fileName: String): Long {
        // Link direto de download com confirm=t para ignorar o prompt de vírus do Google Drive
        val downloadUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
        val uri = Uri.parse(downloadUrl)

        val request = DownloadManager.Request(uri).apply {
            setTitle(fileName)
            setDescription("Baixando jogo via PortZone...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        return downloadManager.enqueue(request)
    }

    /**
     * Consulta o status atual de um download pelo ID.
     */
    fun consultarStatusDownload(downloadId: Long): Int? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1) {
                    return cursor.getInt(statusIndex)
                }
            }
        }
        return null
    }
}
