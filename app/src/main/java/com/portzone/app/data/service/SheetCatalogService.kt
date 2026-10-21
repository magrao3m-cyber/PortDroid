package com.portzone.app.data.service

import android.content.Context
import com.portzone.app.data.model.PortGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class SheetCatalogService(private val context: Context) {

    private val sheetCsvUrl =
        "https://docs.google.com/spreadsheets/d/1wXlykMGC7rQCgebOkA4Awtd4wN9nMHCTkZUFf4KHQ-k/export?format=csv"

    /**
     * Carrega o catálogo de jogos.
     * Tenta buscar a versão mais recente diretamente do Google Sheets.
     * Se falhar (ex: sem internet), utiliza o backup embutido em assets/games_catalog.csv.
     */
    suspend fun carregarCatalogo(forcarLocal: Boolean = false): Result<List<PortGame>> =
        withContext(Dispatchers.IO) {
            try {
                if (!forcarLocal) {
                    try {
                        val jogosRemotos = baixarEParsearRemoto()
                        if (jogosRemotos.isNotEmpty()) {
                            return@withContext Result.success(jogosRemotos)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback para arquivo local
                    }
                }

                val jogosLocais = parsearDeAssets()
                Result.success(jogosLocais)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    private fun baixarEParsearRemoto(): List<PortGame> {
        val url = URL(sheetCsvUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.connect()

        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return conn.inputStream.use { parsearStream(it) }
        }
        throw Exception("Código de resposta HTTP: ${conn.responseCode}")
    }

    private fun parsearDeAssets(): List<PortGame> {
        return context.assets.open("games_catalog.csv").use { parsearStream(it) }
    }

    private fun parsearStream(inputStream: InputStream): List<PortGame> {
        val lista = mutableListOf<PortGame>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var linha = reader.readLine() // Ignora o cabeçalho (GAME,PROJECT,URL,NEEDS GAME FILES?)

        while (true) {
            linha = reader.readLine() ?: break
            if (linha.isBlank()) continue

            val colunas = parsearLinhaCsv(linha)
            if (colunas.size >= 3) {
                val game = colunas.getOrNull(0)?.trim().orEmpty()
                val project = colunas.getOrNull(1)?.trim().orEmpty()
                val projectUrl = colunas.getOrNull(2)?.trim().orEmpty()
                val needsFiles = colunas.getOrNull(3)?.trim().orEmpty()

                if (game.isNotBlank() && projectUrl.isNotBlank()) {
                    lista.add(
                        PortGame(
                            id = UUID.randomUUID().toString(),
                            name = game,
                            project = project,
                            projectUrl = projectUrl,
                            needsGameFiles = needsFiles
                        )
                    )
                }
            }
        }

        return lista
    }

    /**
     * Parser de CSV simples que respeita campos entre aspas contendo vírgulas.
     */
    private fun parsearLinhaCsv(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var dentroDeAspas = false

        for (ch in line) {
            when {
                ch == '"' -> dentroDeAspas = !dentroDeAspas
                ch == ',' && !dentroDeAspas -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
