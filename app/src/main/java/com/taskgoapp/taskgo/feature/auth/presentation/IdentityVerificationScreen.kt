package com.taskgoapp.taskgo.feature.auth.presentation

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(
    onBackClick: () -> Unit,
    onVerificationComplete: () -> Unit,
    viewModel: IdentityVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Verificar permissão de imagem
    val hasImagePermission = remember {
        PermissionHandler.hasImageReadPermission(context)
    }
    
    // Launcher para permissão de imagem
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            // Permissão concedida, continuar com o launcher apropriado
        },
        onPermissionDenied = {
            // Mostrar mensagem de erro
        }
    )
    
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Launchers para seleção de imagens
    val documentFrontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setDocumentFront(it) }
    }
    
    val documentBackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setDocumentBack(it) }
    }
    
    val selfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSelfie(it) }
    }
    
    val addressProofLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAddressProof(it) }
    }
    
    // Função helper para abrir seletor com verificação de permissão
    fun openImageSelector(launcher: androidx.activity.result.ActivityResultLauncher<String>) {
        if (hasImagePermission) {
            launcher.launch("image/*")
        } else {
            pendingAction = { launcher.launch("image/*") }
            imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
        }
    }
    
    // Executar ação pendente após permissão concedida
    LaunchedEffect(hasImagePermission) {
        if (hasImagePermission && pendingAction != null) {
            pendingAction?.invoke()
            pendingAction = null
        }
    }
    
    // Navegação após sucesso
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onVerificationComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Verificação de Identidade",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(TGIcons.Back),
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TaskGoGreen
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TaskGoBackgroundWhite)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Informações
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TaskGoGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Documentos Necessários",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Para sua segurança, precisamos verificar sua identidade. Envie os documentos solicitados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray
                    )
                }
            }
            
            // Documento Frente
            DocumentUploadSection(
                title = "Documento - Frente",
                description = "RG, CNH ou Passaporte",
                imageUri = uiState.documentFrontUri,
                onSelectImage = { openImageSelector(documentFrontLauncher) },
                required = true
            )
            
            // Documento Verso
            DocumentUploadSection(
                title = "Documento - Verso",
                description = "Verso do documento",
                imageUri = uiState.documentBackUri,
                onSelectImage = { openImageSelector(documentBackLauncher) },
                required = true
            )
            
            // Selfie
            DocumentUploadSection(
                title = "Selfie",
                description = "Tire uma selfie segurando seu documento",
                imageUri = uiState.selfieUri,
                onSelectImage = { openImageSelector(selfieLauncher) },
                required = true
            )
            
            // Comprovante de Endereço
            DocumentUploadSection(
                title = "Comprovante de Endereço (Opcional)",
                description = "Conta de luz, água ou telefone",
                imageUri = uiState.addressProofUri,
                onSelectImage = { openImageSelector(addressProofLauncher) },
                required = false
            )
            
            // Mensagem de erro
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botão Enviar
            Button(
                onClick = { viewModel.submitVerification() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                ),
                enabled = !uiState.isLoading &&
                         uiState.documentFrontUri != null &&
                         uiState.documentBackUri != null &&
                         uiState.selfieUri != null
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Enviar Verificação",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DocumentUploadSection(
    title: String,
    description: String,
    imageUri: Uri?,
    onSelectImage: () -> Unit,
    required: Boolean
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    if (required) {
                        Text(
                            text = "* Obrigatório",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color(0xFFF5F5F5),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectImage() },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TaskGoTextGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toque para adicionar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
        }
    }
}
