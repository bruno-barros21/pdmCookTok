package com.example.creation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Helper de FileProvider (Semana 1/OS):
 * Cria um ficheiro temporário na cache privada do contentor da app e devolve um content:// URI seguro.
 */
object CreationMediaFileHelper {
    fun createTempMediaUri(context: Context, mediaType: MediaType): Uri {
        val dir = File(context.cacheDir, "creation_media").apply { mkdirs() }
        val extension = if (mediaType == MediaType.PHOTO) ".jpg" else ".mp4"
        val prefix = if (mediaType == MediaType.PHOTO) "cooktok_photo_" else "cooktok_video_"
        val tempFile = File.createTempFile(prefix, extension, dir)
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, tempFile)
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Ecrã de bloqueio para Visitantes (sem conta).
 * Cumpre: "Utilizadores sem conta não poderão publicar receitas sem criar conta."
 */
@Composable
fun GuestCreationGate(
    onCreateAccountClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Cria conta para publicares receitas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Visitantes podem explorar o Feed e pesquisar receitas, mas apenas utilizadores registados podem publicar novos CookToks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateAccountClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar Conta / Iniciar Sessão")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar Atrás")
        }
    }
}

/**
 * Cartão de seleção e pré-visualização de Foto ou Vídeo Curto.
 * Carrega a miniatura em Dispatchers.IO para nunca bloquear a Main Thread.
 */
@Composable
fun MediaCaptureCard(
    selectedMedia: SelectedMedia?,
    onSelectMediaClick: () -> Unit,
    onRemoveMediaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewBitmap by remember(selectedMedia?.uriString) { mutableStateOf<ImageBitmap?>(null) }
    var videoDurationLabel by remember(selectedMedia?.uriString) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedMedia) {
        previewBitmap = null
        videoDurationLabel = null
        if (selectedMedia == null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(selectedMedia.uriString)
                if (selectedMedia.type == MediaType.PHOTO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                        val bmp = BitmapFactory.decodeStream(stream, null, options)
                        previewBitmap = bmp?.asImageBitmap()
                    }
                } else {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val frame = retriever.getFrameAtTime(0)
                        previewBitmap = frame?.asImageBitmap()
                        val durationMs = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull()
                        if (durationMs != null) {
                            val totalSeconds = (durationMs / 1000).toInt()
                            val mins = totalSeconds / 60
                            val secs = totalSeconds % 60
                            videoDurationLabel = String.format("%d:%02d", mins, secs)
                        }
                    } finally {
                        retriever.release()
                    }
                }
            } catch (_: Exception) {
                // Tratamento defensivo caso a concessão do URI expire ou o emulador não gere miniatura
                previewBitmap = null
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        if (selectedMedia == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectMediaClick)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Selecionar Foto ou Vídeo Curto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para escolher entre a Galeria do telemóvel ou abrir a Câmara em direto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Pré-visualização da receita",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedMedia.type == MediaType.VIDEO) {
                                    Icons.Default.Videocam
                                } else {
                                    Icons.Default.Image
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${selectedMedia.type.label} pronto(a) para publicar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (selectedMedia.type == MediaType.VIDEO) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = videoDurationLabel ?: "Vídeo Curto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }

                    AssistChip(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        label = { Text("${selectedMedia.type.label} • ${selectedMedia.source.label}") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectMediaClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alterar Média")
                    }
                    OutlinedButton(
                        onClick = onRemoveMediaClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remover")
                    }
                }
            }
        }
    }
}

/**
 * UI de Fallback obrigatório (Semana 8 - "100% of denials you must handle"):
 * Surge quando o utilizador recusa a permissão de Câmara, oferecendo um caminho funcional imediato.
 */
@Composable
fun CameraPermissionDeniedFallbackCard(
    onUseGalleryFallbackClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NoPhotography,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acesso à Câmara Indisponível",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sem permissão de câmara não é possível gravar em direto, mas podes escolher qualquer foto ou vídeo curto da tua Galeria sem permissões adicionais, ou ativar a câmara nas Definições.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUseGalleryFallbackClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Escolher da Galeria")
                }
                OutlinedButton(onClick = onOpenAppSettingsClick) {
                    Text("Definições")
                }
            }
        }
    }
}

/**
 * BottomSheet que dá a escolher entre Galeria e Câmara, tanto para Fotografias como para Vídeos Curtos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSourcePickerBottomSheet(
    onPickPhotoFromGallery: () -> Unit,
    onTakePhotoWithCamera: () -> Unit,
    onPickVideoFromGallery: () -> Unit,
    onRecordVideoWithCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Adicionar Conteúdo da Receita",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Fotografia",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            MediaOptionRow(
                icon = Icons.Default.PhotoLibrary,
                title = "Escolher Foto da Galeria",
                subtitle = "Usa o Photo Picker do Android (sem permissões invasivas)",
                onClick = onPickPhotoFromGallery
            )
            MediaOptionRow(
                icon = Icons.Default.PhotoCamera,
                title = "Tirar Foto com a Câmara",
                subtitle = "Captura em direto (requer permissão de Câmara)",
                onClick = onTakePhotoWithCamera
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Vídeo Curto (CookTok)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            MediaOptionRow(
                icon = Icons.Default.VideoLibrary,
                title = "Escolher Vídeo Curto da Galeria",
                subtitle = "Seleciona um vídeo já gravado no dispositivo",
                onClick = onPickVideoFromGallery
            )
            MediaOptionRow(
                icon = Icons.Default.Videocam,
                title = "Gravar Vídeo Curto com a Câmara",
                subtitle = "Gravação em direto (requer permissão de Câmara)",
                onClick = onRecordVideoWithCamera
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MediaOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * Passo 02 do fluxo de permissões da Semana 8: Diálogo de Rationale claro e em contexto.
 */
@Composable
fun CameraPermissionRationaleDialog(
    mediaType: MediaType,
    onConfirmRequest: () -> Unit,
    onFallbackToGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
        title = { Text("Permissão de Câmara Necessária") },
        text = {
            Text(
                text = "O CookTok precisa de acesso à câmara apenas neste momento para ${
                    if (mediaType == MediaType.PHOTO) "tirares a fotografia" else "gravares o vídeo curto"
                } da tua receita. Se preferires não conceder acesso, podes escolher um ficheiro da Galeria."
            )
        },
        confirmButton = {
            Button(onClick = onConfirmRequest) {
                Text("Permitir Câmara")
            }
        },
        dismissButton = {
            TextButton(onClick = onFallbackToGallery) {
                Text("Usar Galeria")
            }
        }
    )
}

/**
 * Formulário completo de detalhes da receita (Stateless / State Hoisting).
 */
@Composable
fun RecipeDetailsFormSection(
    state: CreationUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCuriositiesChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPrepTimeChange: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onDifficultySelected: (RecipeDifficultyOption) -> Unit,
    onAddIngredient: () -> Unit,
    onUpdateIngredient: (String, String?, String?) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onAddStep: () -> Unit,
    onUpdateStep: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Detalhes da Receita",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text("Título da Receita *") },
            placeholder = { Text("Ex: Lasanha de Frango Cremosa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text("Descrição") },
            placeholder = { Text("Breve apresentação do prato...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.curiosities,
            onValueChange = onCuriositiesChange,
            label = { Text("Curiosidades / Dicas do Chef") },
            placeholder = { Text("Ex: Origem do prato ou segredo de confeção...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        // Categoria / Tipo de Conteúdo
        Column {
            Text(
                text = "Categoria",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(defaultCreationCategories) { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(cat) }
                    )
                }
            }
        }

        // Tempo (min) e Porções
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.prepTimeMinutesInput,
                onValueChange = onPrepTimeChange,
                label = { Text("Tempo (min) *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.servingsInput,
                onValueChange = onServingsChange,
                label = { Text("Porções *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
        }

        // Dificuldade
        Column {
            Text(
                text = "Dificuldade",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecipeDifficultyOption.entries.forEach { diff ->
                    FilterChip(
                        selected = state.difficulty == diff,
                        onClick = { onDifficultySelected(diff) },
                        label = { Text(diff.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Ingredientes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ingredientes *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddIngredient) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar")
            }
        }

        state.ingredients.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdateIngredient(item.id, it, null) },
                    label = { Text("Ingrediente ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { onUpdateIngredient(item.id, null, it) },
                    label = { Text("Qtd.") },
                    placeholder = { Text("200g") },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
                if (state.ingredients.size > 1) {
                    IconButton(onClick = { onRemoveIngredient(item.id) }) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Remover ingrediente",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Preparação (Passos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preparação *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddStep) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar Passo")
            }
        }

        state.preparationSteps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = step.instruction,
                    onValueChange = { onUpdateStep(step.id, it) },
                    label = { Text("Passo ${index + 1}") },
                    minLines = 2,
                    modifier = Modifier.weight(1f)
                )
                if (state.preparationSteps.size > 1) {
                    IconButton(
                        onClick = { onRemoveStep(step.id) },
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Remover passo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}