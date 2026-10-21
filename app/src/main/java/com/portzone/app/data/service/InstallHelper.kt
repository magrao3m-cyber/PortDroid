package com.portzone.app.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object InstallHelper {

    /**
     * Abre a tela do instalador de pacotes nativo do Android para o arquivo .apk fornecido.
     * Utiliza o FileProvider para total compatibilidade com Android 7.0 até Android 14+.
     */
    fun instalarApk(context: Context, arquivoApk: File) {
        if (!arquivoApk.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                arquivoApk
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Verifica se um pacote específico já está instalado no sistema operacional.
     */
    fun isPacoteInstalado(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Varre a pasta de downloads e deleta qualquer instalador .apk cujo jogo correspondente
     * já esteja instalado no aparelho do usuário, liberando espaço automaticamente.
     */
    fun limparInstaladoresConcluidos(context: Context, pastaDownloads: File) {
        val packageManager = context.packageManager

        val arquivosNaPasta = pastaDownloads.listFiles() ?: return
        val instaladoresApk = arquivosNaPasta.filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }

        for (arquivoApk in instaladoresApk) {
            try {
                val infoPacote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageArchiveInfo(
                        arquivoApk.absolutePath,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageArchiveInfo(arquivoApk.absolutePath, 0)
                }

                val nomeDoPacote = infoPacote?.packageName

                if (nomeDoPacote != null && isPacoteInstalado(context, nomeDoPacote)) {
                    // Se o jogo já estiver instalado e pronto, deleta o arquivo instalador pesado
                    arquivoApk.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Alias mantido para retrocompatibilidade
    fun limparInstaladoresJaInstalados(context: Context, pastaDownloads: File) {
        limparInstaladoresConcluidos(context, pastaDownloads)
    }
}
