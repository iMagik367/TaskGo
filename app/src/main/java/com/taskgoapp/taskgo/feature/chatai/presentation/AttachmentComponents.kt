package com.taskgoapp.taskgo.feature.chatai.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoBorder
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.feature.chatai.data.AttachmentType
import com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment

@Composable
fun AttachmentPreview(
    attachment: ChatAttachment,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Box {
            when (attachment.type) {
                AttachmentType.IMAGE -> {
                    Image(
                        painter = rememberAsyncImagePainter(attachment.uri),
                        contentDescription = "Imagem anexada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                AttachmentType.DOCUMENT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = TaskGoTextGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = attachment.fileName?.take(10) ?: "Doc",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentSelectionDialog(
    onDismiss: () -> Unit,
    onImageSelected: (Uri, String) -> Unit,
    onDocumentSelected: (Uri, String, String) -> Unit
) {
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "image/*"
            onImageSelected(it, mimeType)
        }
    }
    
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "application/*"
            val fileName = getFileName(context, it)
            onDocumentSelected(it, fileName, mimeType)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar anexo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TaskGoGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Selecionar imagem")
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            documentPickerLauncher.launch("*/*")
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TaskGoGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Selecionar documento")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "documento"
}

