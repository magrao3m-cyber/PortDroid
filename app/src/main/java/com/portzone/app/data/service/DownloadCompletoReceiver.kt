package com.portzone.app.data.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.portzone.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class DownloadCompletoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // 1. Clique na notificação de download do sistema
        if (action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
            val intentApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            try {
                context.startActivity(intentApp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // 2. Conclusão do download em segundo plano
        if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L) return

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusColumnIndex != -1) cursor.getInt(statusColumnIndex) else -1

                val localUriColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val localUriString = if (localUriColumnIndex != -1) cursor.getString(localUriColumnIndex) else null

                if (status == DownloadManager.STATUS_SUCCESSFUL && localUriString != null) {
                    val uri = Uri.parse(localUriString)
                    val arquivoPath = uri.path

                    if (arquivoPath != null) {
                        val arquivoApk = File(arquivoPath)

                        if (arquivoApk.exists() && arquivoApk.name.endsWith(".apk", ignoreCase = true)) {
                            // Dispara o instalador nativo do Android via FileProvider
                            InstallHelper.instalarApk(context, arquivoApk)

                            // Notifica o ViewModel com goAsync() para manter o processo ativo
                            val pendingResult = goAsync()
                            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                            scope.launch {
                                try {
                                    DownloadTracker.notificarConclusao(arquivoApk.name)
                                } finally {
                                    pendingResult.finish()
                                }
                            }
                        }
                    }
                }
                cursor.close()
            }
        }
    }
}
