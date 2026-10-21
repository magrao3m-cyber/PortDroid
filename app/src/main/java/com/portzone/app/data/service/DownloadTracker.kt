package com.portzone.app.data.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DownloadTracker {
    private val _downloadConcluidoEvent = MutableSharedFlow<String>() // Transmite o nome do arquivo concluído
    val downloadConcluidoEvent = _downloadConcluidoEvent.asSharedFlow()

    suspend fun notificarConclusao(nomeArquivo: String) {
        _downloadConcluidoEvent.emit(nomeArquivo)
    }
}
