package com.portzone.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.portzone.app.data.saf.ComplementaryFileInfo
import com.portzone.app.data.saf.SafGameDataManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DialogoGerenciadorDeDados(
    pastaDownloads: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var arquivosComplementares by remember {
        mutableStateOf(SafGameDataManager.listarArquivosComplementaresLocais(pastaDownloads))
    }

    var arquivoSelecionadoParaCopia by remember { mutableStateOf<ComplementaryFileInfo?>(null) }
    var copiando by remember { mutableStateOf(false) }
    var progressoCopia by remember { mutableFloatStateOf(0f) }

    // Launcher para abrir a árvore de diretórios via SAF
    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null && arquivoSelecionadoParaCopia != null) {
            val arq = arquivoSelecionadoParaCopia!!.arquivo
            copiando = true
            progressoCopia = 0f

            // Persiste a permissão de acesso para que o app não precise perguntar novamente
            SafGameDataManager.salvarUriPersistente(context, treeUri, arq.nameWithoutExtension)

            coroutineScope.launch {
                val sucesso = SafGameDataManager.copiarArquivoParaSafTree(
                    context = context,
                    treeUri = treeUri,
                    arquivoOrigem = arq,
                    onProgresso = { progressoCopia = it }
                )

                copiando = false
                arquivoSelecionadoParaCopia = null

                if (sucesso) {
                    Toast.makeText(context, "✅ Arquivos copiados com sucesso para a pasta do jogo!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ Falha ao copiar arquivos para a pasta selecionada.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF1F2833)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Cabeçalho do Diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SdCard,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dados & ROMs (SAF)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ports de PC e emuladores precisam de arquivos complementares (.WAD, .PAK, .Z64, .ZIP). O SAF copia diretamente para as pastas de jogos do sistema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progresso de cópia em andamento
                AnimatedVisibility(visible = copiando) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Transferindo arquivos...",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "${(progressoCopia * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressoCopia },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF0B0C10)
                        )
                    }
                }

                if (arquivosComplementares.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum arquivo .WAD, .PAK ou ROM detectado em Downloads.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(arquivosComplementares) { item ->
                            ItemArquivoComplementar(
                                item = item,
                                habilitado = !copiando,
                                onCopiarClick = {
                                    arquivoSelecionadoParaCopia = item
                                    safFolderLauncher.launch(null)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B0C10),
                        contentColor = Color.White
                    )
                ) {
                    Text("Concluir")
                }
            }
        }
    }
}

@Composable
private fun ItemArquivoComplementar(
    item: ComplementaryFileInfo,
    habilitado: Boolean,
    onCopiarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0C10))
            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.extensao.uppercase(),
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.arquivo.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${item.destinoSugerido.nomeJogo} • ${item.tamanhoFormatado}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledTonalButton(
            onClick = onCopiarClick,
            enabled = habilitado,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
