package com.portzone.app.data.model

import java.util.Locale

enum class StatusJogo {
    DISPONIVEL_DRIVE,       // Não baixado
    BAIXANDO,               // Download em progresso pelo sistema
    PRONTO_PARA_INSTALAR,   // APK já existe na pasta de downloads
    ATUALIZACAO_DISPONIVEL  // O APK existe, mas o do Drive é mais novo
}

data class JogoPort(
    val id: String,
    val nome: String,
    val tamanho: Long?,
    val dataModificacaoDrive: Long = 0L, // Guardaremos o timestamp do Drive para checar atualizações
    val categoria: String = "Geral",      // Para organizar em abas
    val urlCapa: String? = null,          // URL da imagem de capa (.jpg ou .png com mesmo nome)
    val statusLocal: StatusJogo = StatusJogo.DISPONIVEL_DRIVE,
    val progresso: Float = 0.0f,          // De 0.0f a 1.0f
    val estaBaixando: Boolean = false,     // Controla se mostra a barra ou o botão
    val isNovo: Boolean = false,          // Marca ports recém-adicionados / novos na varredura
    val mimeType: String? = null
) {
    val tamanhoFormatado: String
        get() {
            if (tamanho == null || tamanho <= 0) return "Tamanho desconhecido"
            val kb = tamanho / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }
}
