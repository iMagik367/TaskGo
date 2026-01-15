package com.taskgoapp.taskgo.feature.feed.presentation.components

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher
import com.taskgoapp.taskgo.core.permissions.rememberCameraPermissionLauncher
import com.taskgoapp.taskgo.feature.feed.presentation.StoriesViewModel
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Data class para representar um overlay de texto/emoji sobre a imagem
 */
data class TextOverlay(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val color: Color = Color.White,
    val fontSize: Float = 24f
)

/**
 * Tela de criação de Story idêntica ao Instagram
 * Fluxo simplificado em 2 páginas:
 * 1. Câmera/Galeria (captura ou seleção de mídia)
 * 2. Edição (texto sobre imagem, stickers, música, filtros, compartilhamento)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onDismiss: () -> Unit,
    onStoryCreated: () -> Unit,
    viewModel: StoriesViewModel = hiltViewModel()
) {
    // Estados principais
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isEditingMode by remember { mutableStateOf(false) }
    
    // Estados para texto sobre a imagem
    var textOverlays by remember { mutableStateOf<List<TextOverlay>>(emptyList()) }
    var activeTextOverlayId by remember { mutableStateOf<String?>(null) }
    
    // Estados para ferramentas
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var showFiltersPicker by remember { mutableStateOf(false) }
    var showStickerMenu by remember { mutableStateOf(false) }
    
    // Estado para input de texto direto sobre a imagem
    var isTextInputActive by remember { mutableStateOf(false) }
    var currentTextInput by remember { mutableStateOf("") }
    var currentTextInputPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    // Verificar permissões
    val hasImagePermission by remember {
        derivedStateOf {
        PermissionHandler.hasImageReadPermission(context)
        }
    }
    
    val hasCameraPermission by remember {
        derivedStateOf {
            PermissionHandler.hasCameraPermission(context)
        }
    }
    
    // Declarar mediaLauncher ANTES de imagePermissionLauncher para evitar referência não resolvida
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            isEditingMode = true
        }
    }
    
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            // Após permissão concedida, abrir galeria automaticamente
            mediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        },
        onPermissionDenied = {
            android.util.Log.w("CreateStoryScreen", "Permissão de acesso à galeria negada")
        }
    )
    
    val cameraPermissionLauncher = com.taskgoapp.taskgo.core.permissions.rememberCameraPermissionLauncher(
        onPermissionGranted = {
            // Após permissão concedida, a câmera customizada será mostrada automaticamente
            android.util.Log.d("CreateStoryScreen", "Permissão de câmera concedida")
        },
        onPermissionDenied = {
            android.util.Log.w("CreateStoryScreen", "Permissão de câmera negada")
        }
    )
    
    // Handler para quando foto é capturada pela câmera customizada
    val onPhotoCaptured: (Uri) -> Unit = { uri ->
        android.util.Log.d("CreateStoryScreen", "Foto capturada pela câmera customizada: $uri")
        selectedMediaUri = uri
        isEditingMode = true
    }
    
    // Solicitar permissão de câmera quando o Dialog aparecer (se necessário)
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            android.util.Log.d("CreateStoryScreen", "Solicitando permissão de câmera")
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                // PÁGINA 1: Câmera customizada (estilo Instagram) - abre diretamente
                !isEditingMode && selectedMediaUri == null -> {
                    if (hasCameraPermission) {
                        // Mostrar câmera customizada diretamente
                        CustomCameraView(
                            onPhotoCaptured = onPhotoCaptured,
                            onDismiss = onDismiss,
                            onGalleryClick = {
                                // Abrir galeria quando o botão for clicado
                                if (hasImagePermission) {
                                    mediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                } else {
                                    imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Se não tem permissão, mostrar opção de galeria ou solicitar permissão
                        CameraGalleryPage(
                            onCameraClick = {
                                // Solicitar permissão de câmera
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            onGalleryClick = {
                                if (hasImagePermission) {
                                    mediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                } else {
                                    imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                                }
                            },
                            onClose = onDismiss
                        )
                    }
                }
                
                // PÁGINA 2: Edição
                selectedMediaUri != null -> {
                    EditingPage(
                        mediaUri = selectedMediaUri!!,
                        textOverlays = textOverlays,
                        activeTextOverlayId = activeTextOverlayId,
                        onTextOverlayAdded = { text ->
                            val newOverlay = TextOverlay(
                                id = System.currentTimeMillis().toString(),
                                text = text,
                                x = 0.5f,
                                y = 0.5f
                            )
                            textOverlays = textOverlays + newOverlay
                            activeTextOverlayId = newOverlay.id
                        },
                        onTextOverlayUpdated = { id, x, y ->
                            textOverlays = textOverlays.map { overlay ->
                                if (overlay.id == id) overlay.copy(x = x, y = y) else overlay
                            }
                        },
                        onTextOverlayDeleted = { id ->
                            textOverlays = textOverlays.filter { it.id != id }
                            if (activeTextOverlayId == id) {
                                activeTextOverlayId = null
                            }
                        },
                        onTextOverlayTapped = { id ->
                            activeTextOverlayId = if (activeTextOverlayId == id) null else id
                        },
                        onTextOverlayScaleChanged = { id, scale ->
                            textOverlays = textOverlays.map { overlay ->
                                if (overlay.id == id) overlay.copy(scale = scale) else overlay
                            }
                        },
                        onTextOverlayFontSizeChanged = { id, fontSize ->
                            textOverlays = textOverlays.map { overlay ->
                                if (overlay.id == id) overlay.copy(fontSize = fontSize) else overlay
                            }
                        },
                        onShowTextInput = {
                            isTextInputActive = true
                            currentTextInputPosition = Pair(0.5f, 0.5f)
                        },
                        onShowEmojiPicker = { 
                            showEmojiPicker = true
                            showStickerMenu = true
                        },
                        onShowMusicPicker = { showMusicPicker = true },
                        onShowFiltersPicker = { showFiltersPicker = true },
                        onBack = {
                            isEditingMode = false
                            textOverlays = emptyList()
                            activeTextOverlayId = null
                        },
                        onShare = { caption ->
                            scope.launch {
                                try {
                                    android.util.Log.d("CreateStoryScreen", "=== INICIANDO CRIAÇÃO DE STORY ===")
                                    val mediaUri = selectedMediaUri
                                    android.util.Log.d("CreateStoryScreen", "URI selecionada: $mediaUri")
                                    
                                    if (mediaUri == null) {
                                        android.util.Log.e("CreateStoryScreen", "ERRO: selectedMediaUri é null!")
                                        return@launch
                                    }
                                    
                                    val mediaType = try {
                                        val mimeType = context.contentResolver.getType(mediaUri) ?: "image/jpeg"
                                        val detectedType = if (mimeType.startsWith("video/")) "video" else "image"
                                        android.util.Log.d("CreateStoryScreen", "MIME type: $mimeType, Tipo detectado: $detectedType")
                                        detectedType
                                    } catch (e: Exception) {
                                        android.util.Log.w("CreateStoryScreen", "Erro ao detectar tipo de mídia: ${e.message}", e)
                                        "image"
                                    }
                                    
                                    android.util.Log.d("CreateStoryScreen", "Chamando viewModel.createStory com mediaType=$mediaType, caption=null")
                                    
                                    val result = viewModel.createStory(
                                        mediaUri = mediaUri,
                                        mediaType = mediaType,
                                        caption = null // Sempre null - texto é escrito em cima da foto
                                    )
                                    
                                    android.util.Log.d("CreateStoryScreen", "Resultado recebido: $result")
                                    
                                    when (result) {
                                        is com.taskgoapp.taskgo.core.model.Result.Success -> {
                                            android.util.Log.d("CreateStoryScreen", "✅ Story criada com sucesso! ID: ${result.data}")
                                            // Recarregar stories para garantir que a nova story apareça
                                            viewModel.loadStories()
                                            onStoryCreated()
                                            onDismiss()
                                        }
                                        is com.taskgoapp.taskgo.core.model.Result.Error -> {
                                            android.util.Log.e("CreateStoryScreen", "❌ Erro ao criar story: ${result.exception.message}", result.exception)
                                            android.util.Log.e("CreateStoryScreen", "Stack trace:", result.exception)
                                            // Erro será mostrado no UI state do ViewModel
                                        }
                                        else -> {
                                            android.util.Log.w("CreateStoryScreen", "⚠️ Resultado desconhecido ao criar story: $result")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CreateStoryScreen", "❌ Exceção ao criar story: ${e.message}", e)
                                    android.util.Log.e("CreateStoryScreen", "Stack trace completo:", e)
                                }
                            }
                        },
                        isLoading = uiState.isLoading,
                        isTextInputActive = isTextInputActive,
                        currentTextInput = currentTextInput,
                        onTextInputChanged = { currentTextInput = it },
                        onTextInputConfirmed = {
                            if (currentTextInput.isNotBlank()) {
                                val newOverlay = TextOverlay(
                                    id = System.currentTimeMillis().toString(),
                                    text = currentTextInput,
                                    x = currentTextInputPosition?.first ?: 0.5f,
                                    y = currentTextInputPosition?.second ?: 0.5f
                                )
                                textOverlays = textOverlays + newOverlay
                                activeTextOverlayId = newOverlay.id
                            }
                            isTextInputActive = false
                            currentTextInput = ""
                            currentTextInputPosition = null
                        },
                        onTextInputDismissed = {
                            isTextInputActive = false
                            currentTextInput = ""
                            currentTextInputPosition = null
                        },
                        textInputPosition = currentTextInputPosition
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showEmojiPicker) {
        EmojiPickerDialog(
            onEmojiSelected = { emoji ->
                val newOverlay = TextOverlay(
                    id = System.currentTimeMillis().toString(),
                    text = emoji,
                    x = 0.5f,
                    y = 0.5f
                )
                textOverlays = textOverlays + newOverlay
                activeTextOverlayId = newOverlay.id
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }
    
    if (showMusicPicker) {
        MusicPickerDialog(
            onDismiss = { showMusicPicker = false }
        )
    }
    
    if (showFiltersPicker) {
        FiltersPickerDialog(
            onDismiss = { showFiltersPicker = false }
        )
    }
}

/**
 * Página 1: Câmera/Galeria
 */
@Composable
private fun CameraGalleryPage(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Botão X (topo esquerdo)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Botão Galeria (topo direito)
        IconButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = "Galeria",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Botão Câmera (centro)
        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
                    .background(Color.Black, CircleShape)
            )
        }
    }
}

/**
 * Página 2: Edição
 */
@Composable
private fun EditingPage(
    mediaUri: Uri,
    textOverlays: List<TextOverlay>,
    activeTextOverlayId: String?,
    onTextOverlayAdded: (String) -> Unit,
    onTextOverlayUpdated: (String, Float, Float) -> Unit,
    onTextOverlayDeleted: (String) -> Unit,
    onTextOverlayTapped: (String) -> Unit,
    onTextOverlayScaleChanged: (String, Float) -> Unit,
    onTextOverlayFontSizeChanged: (String, Float) -> Unit,
    onShowTextInput: () -> Unit,
    onShowEmojiPicker: () -> Unit,
    onShowMusicPicker: () -> Unit,
    onShowFiltersPicker: () -> Unit,
    onBack: () -> Unit,
    onShare: (String) -> Unit,
    isLoading: Boolean,
    isTextInputActive: Boolean,
    currentTextInput: String,
    onTextInputChanged: (String) -> Unit,
    onTextInputConfirmed: () -> Unit,
    onTextInputDismissed: () -> Unit,
    textInputPosition: Pair<Float, Float>?
) {
    // Caption removido - texto é escrito diretamente em cima da foto
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    
    LaunchedEffect(isTextInputActive) {
        if (isTextInputActive) {
            focusRequester.requestFocus()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Preview da mídia (full-screen)
        AsyncImage(
            model = mediaUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Overlay preto semi-transparente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.1f))
        )
        
        // Textos sobre a imagem
        TextOverlaysRenderer(
            overlays = textOverlays,
            activeTextOverlayId = activeTextOverlayId,
            onTextOverlayTapped = onTextOverlayTapped,
            onTextOverlayUpdated = onTextOverlayUpdated,
            onTextOverlayScaleChanged = onTextOverlayScaleChanged,
            onTextOverlayFontSizeChanged = onTextOverlayFontSizeChanged,
            onTextOverlayDeleted = onTextOverlayDeleted
        )
        
        // Controles de tamanho de fonte quando um overlay está ativo (estilo Instagram)
        activeTextOverlayId?.let { activeId ->
            val activeOverlay = textOverlays.find { it.id == activeId }
            activeOverlay?.let { overlay ->
                // Controles na parte inferior da tela
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp) // Acima do bottom bar
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão diminuir tamanho
                        IconButton(
                            onClick = {
                                val newSize = (overlay.fontSize - 4f).coerceAtLeast(12f)
                                onTextOverlayFontSizeChanged(overlay.id, newSize)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "A-",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Tamanho atual
                        Text(
                            text = "${overlay.fontSize.toInt()}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(30.dp)
                        )
                        
                        // Botão aumentar tamanho
                        IconButton(
                            onClick = {
                                val newSize = (overlay.fontSize + 4f).coerceAtMost(72f)
                                onTextOverlayFontSizeChanged(overlay.id, newSize)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "A+",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Input de texto direto sobre a imagem (estilo Instagram)
        if (isTextInputActive && textInputPosition != null) {
            val density = LocalDensity.current
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = with(density) { ((textInputPosition.first - 0.5f) * maxWidth.value).toDp() },
                            y = with(density) { ((textInputPosition.second - 0.5f) * maxHeight.value).toDp() }
                        )
                        .widthIn(max = maxWidth * 0.8f)
                ) {
                    TextField(
                        value = currentTextInput,
                        onValueChange = onTextInputChanged,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .background(Color.Transparent),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (currentTextInput.isNotBlank()) {
                                    onTextInputConfirmed()
                                } else {
                                    onTextInputDismissed()
                                }
                            }
                        ),
                        singleLine = false,
                        maxLines = 5
                    )
                }
            }
        }
        
        // TOP BAR
        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botão voltar
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Voltar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Botão Galeria
            IconButton(
                onClick = { /* Abrir galeria novamente */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "Galeria",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // SIDE BAR - Ferramentas (direita)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Texto (Aa)
            IconButton(
                onClick = onShowTextInput,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Texto",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Stickers (emoji)
            IconButton(
                onClick = onShowEmojiPicker,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Text(
                    text = "😊",
                    fontSize = 24.sp
                )
            }
            
            // Música
            IconButton(
                onClick = onShowMusicPicker,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Música",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Filtros
            IconButton(
                onClick = onShowFiltersPicker,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Filtros",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // BOTTOM BAR - Compartilhamento (sem campo de legenda - texto é escrito em cima da foto)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botão compartilhar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onShare("") }, // Sem legenda - texto é escrito em cima da foto
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                        Text(
                            text = "Seus stories",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                IconButton(
                    onClick = { onShare("") }, // Sem legenda - texto é escrito em cima da foto
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Compartilhar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renderizador de overlays de texto
 */
@Composable
private fun TextOverlaysRenderer(
    overlays: List<TextOverlay>,
    activeTextOverlayId: String?,
    onTextOverlayTapped: (String) -> Unit,
    onTextOverlayUpdated: (String, Float, Float) -> Unit,
    onTextOverlayScaleChanged: (String, Float) -> Unit,
    onTextOverlayFontSizeChanged: (String, Float) -> Unit,
    onTextOverlayDeleted: (String) -> Unit
) {
    overlays.forEach { overlay ->
        key(overlay.id) {
            TextOverlayItem(
                overlay = overlay,
                isActive = overlay.id == activeTextOverlayId,
                onTap = { onTextOverlayTapped(overlay.id) },
                onDrag = { x, y -> onTextOverlayUpdated(overlay.id, x, y) },
                onScaleChanged = { scale -> onTextOverlayScaleChanged(overlay.id, scale) },
                onFontSizeChanged = { fontSize -> onTextOverlayFontSizeChanged(overlay.id, fontSize) },
                onDelete = { onTextOverlayDeleted(overlay.id) }
            )
        }
    }
}

/**
 * Item de texto/emoji sobre a imagem (estilo Instagram)
 * Suporta: drag, pinch-to-zoom, drag-to-delete com ícone de lixeira
 */
@Composable
private fun TextOverlayItem(
    overlay: TextOverlay,
    isActive: Boolean,
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember(overlay.x) { mutableStateOf<Float>(overlay.x) }
    var offsetY by remember(overlay.y) { mutableStateOf<Float>(overlay.y) }
    var currentScale by remember(overlay.scale) { mutableStateOf<Float>(overlay.scale) }
    var dragToDeleteProgress by remember { mutableStateOf(0f) } // 0f = normal, 1f = sobre lixeira
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight
        val trashIconY = maxHeight.value * 0.9f // Posição da lixeira (90% da altura)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(overlay.id, isActive, maxHeight.value) {
                    if (isActive) {
                        // Usar detectTransformGestures primeiro (prioridade para pinch-to-zoom)
                        detectTransformGestures(
                            onGesture = { centroid, pan, zoom, rotation ->
                                // Se é um gesto de zoom (pinch), atualizar escala
                                if (zoom != 1f) {
                                    val newScale = (currentScale * zoom).coerceIn(0.5f, 3f)
                                    currentScale = newScale
                                    onScaleChanged(newScale)
                                    // Resetar drag-to-delete durante pinch
                                    dragToDeleteProgress = 0f
                                }
                                // Se há pan durante transform, mover o objeto
                                if (pan.x != 0f || pan.y != 0f) {
                                    val newX = (offsetX + pan.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val newY = (offsetY + pan.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    offsetX = newX
                                    offsetY = newY
                                    onDrag(offsetX, offsetY)
                                    // Resetar drag-to-delete durante pinch
                                    dragToDeleteProgress = 0f
                                }
                            }
                        )
                        
                        // Drag simples para mover e deletar (apenas com 1 dedo)
                        detectDragGestures(
                            onDragStart = { 
                                dragToDeleteProgress = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                
                                // Calcular nova posição primeiro
                                val newX = (offsetX + dragAmount.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val newY = (offsetY + dragAmount.y / size.height.toFloat()).coerceIn(0f, 1f)
                                
                                // Calcular posição absoluta na tela APÓS aplicar o drag
                                val absoluteYAfterDrag = newY * size.height
                                val deleteZoneHeight = size.height * 0.10f // 10% da altura como zona de delete (área da lixeira)
                                val deleteZoneTop = trashIconY - deleteZoneHeight
                                val deleteZoneBottom = trashIconY + deleteZoneHeight
                                
                                // Verificar se a posição APÓS o drag está dentro da zona de delete (área inferior onde fica a lixeira)
                                val isInDeleteZone = absoluteYAfterDrag >= deleteZoneTop && absoluteYAfterDrag <= deleteZoneBottom
                                
                                if (isInDeleteZone) {
                                    // Dentro da zona de delete - calcular progresso baseado na distância do centro da lixeira
                                    val distanceFromTrashCenter = kotlin.math.abs(absoluteYAfterDrag - trashIconY)
                                    dragToDeleteProgress = (1f - (distanceFromTrashCenter / deleteZoneHeight).coerceIn(0f, 1f))
                                } else {
                                    // Fora da zona de delete - resetar progresso
                                    dragToDeleteProgress = 0f
                                }
                                
                                // Atualizar posição durante o drag
                                offsetX = newX
                                offsetY = newY
                                onDrag(offsetX, offsetY)
                            },
                            onDragEnd = {
                                // Deletar APENAS se estava claramente sobre a lixeira (na zona de delete) ao soltar
                                if (dragToDeleteProgress >= 0.8f) {
                                    onDelete()
                                }
                                // Resetar estado após o drag
                                    dragToDeleteProgress = 0f
                                }
                        )
                    } else {
                        detectTapGestures {
                            onTap()
                        }
                    }
                }
        ) {
            // Ícone de lixeira (aparece quando arrasta para baixo)
            if (isActive && dragToDeleteProgress > 0.1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-16).dp)
                        .alpha(dragToDeleteProgress)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Arraste para excluir",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (dragToDeleteProgress > 0.8f) Color.Red else Color.Black.copy(alpha = 0.7f),
                                CircleShape
                            )
                            .padding(12.dp)
                    )
                }
            }
            
            // Texto/emoji posicionado
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = with(density) { ((offsetX - 0.5f) * maxWidth.value).toDp() },
                        y = with(density) { ((offsetY - 0.5f) * maxHeight.value).toDp() }
                    )
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        alpha = if (dragToDeleteProgress > 0.1f) 1f - dragToDeleteProgress * 0.5f else 1f
                    }
                    .background(
                        if (isActive) Color.Black.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
                    .clickable { onTap() }
            ) {
                Text(
                    text = overlay.text,
                    color = overlay.color,
                    fontSize = (overlay.fontSize * currentScale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Dialog para adicionar texto
 */
@Composable
private fun TextEditorDialog(
    onTextAdded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Texto") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 100) text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Digite o texto") },
                maxLines = 3,
                placeholder = { Text("Digite aqui...") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            onTextAdded(text)
                        }
                    }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onTextAdded(text)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Dialog para selecionar emojis (design profissional)
 */
@Composable
private fun EmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojiCategories = remember {
        mapOf(
            "Faces" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏"),
            "Corações" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟"),
            "Mãos" to listOf("👍", "👎", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘", "👌", "🤌", "🤏", "👈", "👉", "👆", "👇", "☝️", "👋", "🤚", "🖐️", "✋", "🖖", "👏", "🙌", "🤲", "🤝", "🙏"),
            "Objetos" to listOf("📱", "💻", "⌚", "📷", "📹", "🎥", "📺", "📻", "🎙️", "🎚️", "🎛️", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳", "📡", "🔋", "🔌", "💡", "🔦", "🕯️", "🧯", "🛢️", "💸", "💵", "💴", "💶", "💷", "💰", "💳", "💎", "⚖️", "🛠️", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "🔩", "⚙️", "🧰", "🧲", "🔫", "💣", "🧨", "🔪", "🗡️", "⚔️", "🛡️", "🚬", "⚰️", "⚱️", "🏺", "🔮", "📿", "🧿", "💈", "⚗️", "🔭", "🔬", "🕳️", "🩹", "🩺", "💊", "💉", "🩸", "🧬", "🦠", "🧫", "🧪", "🌡️", "🧹", "🪠", "🧺", "🧻", "🚽", "🚿", "🛁", "🛀", "🧼", "🪥", "🪒", "🧽", "🪣", "🧴", "🛎️", "🔑", "🗝️", "🚪", "🪑", "🛋️", "🛏️", "🛌", "🧸", "🪆", "🖼️", "🪞", "🪟", "🛍️", "🛒", "🎁", "🎈", "🎉", "🎊", "🎀", "🎗️", "🏆", "🥇", "🥈", "🥉", "⚽", "⚾", "🥎", "🏀", "🏐", "🏈", "🥏", "🎾", "🏉", "🥍", "🏓", "🏸", "🏒", "🏑", "🥌", "⛳", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛷", "⛸️", "🥌", "🎿", "⛷️", "🏂", "🪂", "🏋️", "🤼", "🤸", "🤺", "⛹️", "🤹", "🧘", "🏄", "🏊", "🤽", "🚣", "🧗", "🚵", "🚴", "🏇", "🤾", "🏌️", "🏇", "🧘", "🏄", "🏊", "🤽", "🚣", "🧗", "🚵", "🚴", "🏇", "🤾", "🏌️")
        )
    }
    
    var selectedCategory by remember { mutableStateOf("Faces") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Selecionar Emoji",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Categorias
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojiCategories.keys.toList()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 12.sp) }
                        )
                    }
                }
                
                // Grid de emojis
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(emojiCategories[selectedCategory] ?: emptyList()) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onEmojiSelected(emoji) }
                                .background(
                                    Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 28.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

/**
 * Dialog placeholder para música
 */
@Composable
private fun MusicPickerDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Música") },
        text = {
            Text(
                "A seleção de música será implementada em breve."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * Dialog placeholder para filtros
 */
@Composable
private fun FiltersPickerDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros") },
        text = {
            Text(
                "A seleção de filtros será implementada em breve."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
