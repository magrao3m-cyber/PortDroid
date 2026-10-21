package com.portzone.app.data.service

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Gerenciador de Credenciais com criptografia AES-256 e chaves geradas diretamente
 * no Android Keystore (chip de segurança físico do hardware do dispositivo).
 *
 * Evita a exposição de credenciais em texto puro (.json) no APK final ou no sistema de arquivos.
 */
object GerenciadorCredenciaisSeguras {

    const val NOME_ARQUIVO_PADRAO = "credentials.enc"

    // Gera ou recupera uma chave mestra guardada no chip de segurança (Keystore) do celular
    private fun obterChaveMestra(): String {
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    /**
     * Salva o JSON original transformando-o em um arquivo criptografado (.enc)
     * protegido por hardware.
     */
    fun salvarCredenciaisCriptografadas(
        context: Context,
        jsonTextoPuro: String,
        nomeArquivoDestino: String = NOME_ARQUIVO_PADRAO
    ) {
        val arquivoDestino = File(context.filesDir, nomeArquivoDestino)

        // Se já existir, deleta para sobrescrever com segurança
        if (arquivoDestino.exists()) {
            arquivoDestino.delete()
        }

        val encryptedFile = EncryptedFile.Builder(
            arquivoDestino,
            context,
            obterChaveMestra(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val outputStream: OutputStream = encryptedFile.openFileOutput()
        outputStream.write(jsonTextoPuro.toByteArray(Charsets.UTF_8))
        outputStream.flush()
        outputStream.close()
    }

    /**
     * Descriptografa o arquivo em tempo de execução diretamente na memória RAM do app.
     * Nenhum arquivo temporário em texto puro é salvo no armazenamento.
     */
    fun lerCredenciaisCriptografadas(
        context: Context,
        nomeArquivo: String = NOME_ARQUIVO_PADRAO
    ): String {
        val arquivoCriptografado = File(context.filesDir, nomeArquivo)
        if (!arquivoCriptografado.exists()) return ""

        return try {
            val encryptedFile = EncryptedFile.Builder(
                arquivoCriptografado,
                context,
                obterChaveMestra(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val inputStream: InputStream = encryptedFile.openFileInput()
            val byteArray = inputStream.readBytes()
            inputStream.close()

            String(byteArray, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Verifica se o arquivo seguro criptografado já existe no diretório privado do aplicativo.
     */
    fun existeCredencialCriptografada(
        context: Context,
        nomeArquivo: String = NOME_ARQUIVO_PADRAO
    ): Boolean {
        return File(context.filesDir, nomeArquivo).exists()
    }
}
