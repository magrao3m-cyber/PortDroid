package com.portzone.app.ui

import com.portzone.app.data.model.JogoPort

sealed interface TelaJogosUiState {
    object Carregando : TelaJogosUiState
    data class Sucesso(val jogos: List<JogoPort>) : TelaJogosUiState
    data class Erro(val mensagem: String) : TelaJogosUiState
}
