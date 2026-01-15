package com.taskgoapp.taskgo.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.taskgoapp.taskgo.core.model.Result
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class FeedMediaRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val authRepository: FirebaseAuthRepository,
    @ApplicationContext private val context: Context
) {
    
    private val postsStoragePath = "posts"
    private val storiesStoragePath = "stories"
    
    /**
     * Faz upload de uma mídia (imagem ou vídeo) para o Firebase Storage
     * Segue a mesma lógica de produtos e serviços: sempre usa o userId do usuário autenticado
     * @param uri URI local da mídia
     * @param userId ID do usuário (deve corresponder ao usuário autenticado)
     * @param mediaType "image" ou "video"
     * @return URL pública da mídia no Storage
     */
    suspend fun uploadPostMedia(
        uri: Uri,
        userId: String,
        mediaType: String
    ): Result<String> {
        // Gerar nome único para o arquivo (fora do try para estar acessível no catch)
        val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}"
        val extension = try {
            getFileExtension(uri, mediaType)
        } catch (e: Exception) {
            android.util.Log.w("FeedMediaRepository", "Erro ao obter extensão do arquivo: ${e.message}")
            when (mediaType) {
                "video" -> "mp4"
                else -> "jpg"
            }
        }
        val fullFileName = "$fileName.$extension"
        
        return try {
            // CRÍTICO: Sempre obter userId do usuário autenticado (mesma lógica de produtos/serviços)
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                android.util.Log.e("FeedMediaRepository", "Usuário não autenticado")
                return Result.Error(Exception("Usuário não autenticado"))
            }
            
            // Usar sempre o userId do usuário autenticado para garantir permissões corretas
            val authenticatedUserId = currentUser.uid
            
            // Validar que o userId passado corresponde ao autenticado (segurança adicional)
            if (userId != authenticatedUserId) {
                android.util.Log.e("FeedMediaRepository", "UserId passado ($userId) não corresponde ao autenticado ($authenticatedUserId)")
                return Result.Error(Exception("Permissão negada: userId não corresponde ao usuário autenticado"))
            }
            
            android.util.Log.d("FeedMediaRepository", "Upload de post - userId: $authenticatedUserId, path: posts/$authenticatedUserId/$fullFileName")
            
            // Criar referência no Storage: posts/{userId}/{fileName} (mesma estrutura de produtos/serviços)
            val storageRef: StorageReference = storage.reference
                .child(postsStoragePath)
                .child(authenticatedUserId)
                .child(fullFileName)
            
            android.util.Log.d("FeedMediaRepository", "Storage path criado: posts/$authenticatedUserId/$fullFileName")
            
            // Validar tamanho do arquivo antes do upload (limite de 50MB conforme regras do Storage)
            // Usar abordagem segura para evitar problemas durante análise estática do R8
            val fileSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    pfd.statSize
                } ?: 0L
            } catch (e: Exception) {
                android.util.Log.w("FeedMediaRepository", "Erro ao obter tamanho do arquivo: ${e.message}", e)
                0L
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("FeedMediaRepository", "OutOfMemoryError ao obter tamanho do arquivo: ${e.message}", e)
                0L
            } catch (e: Throwable) {
                android.util.Log.e("FeedMediaRepository", "Erro inesperado ao obter tamanho do arquivo: ${e.message}", e)
                0L
            }
            
            // Calcular tamanho em MB para validação e exibição
            val fileSizeMB = if (fileSize > 0) {
                val mb = fileSize / (1024 * 1024)
                mb
            } else {
                0
            }
            val maxSizeMB = 50 * 1024 * 1024
            
            if (fileSize <= 0) {
                android.util.Log.w("FeedMediaRepository", "Não foi possível determinar o tamanho do arquivo. Continuando com upload...")
            } else if (fileSize > maxSizeMB) {
                // Calcular tamanho formatado para mensagem de erro
                val fileSizeMBDouble = fileSize.toDouble() / (1024.0 * 1024.0)
                val fileSizeMBFormatted = try {
                    String.format(java.util.Locale.US, "%.2f", fileSizeMBDouble)
                } catch (e: Exception) {
                    fileSizeMB.toString()
                }
                android.util.Log.e("FeedMediaRepository", "Arquivo muito grande: ${fileSizeMB}MB. Limite: 50MB")
                return Result.Error(Exception("Arquivo muito grande. Limite máximo: 50MB. Tamanho do arquivo: ${fileSizeMBFormatted}MB"))
            } else {
                android.util.Log.d("FeedMediaRepository", "Tamanho do arquivo validado: ${fileSizeMB}MB")
            }
            
            // Detectar contentType real do arquivo
            val detectedMimeType = try {
                context.contentResolver.getType(uri)
            } catch (e: Exception) {
                android.util.Log.w("FeedMediaRepository", "Erro ao obter MIME type: ${e.message}", e)
                null
            }
            
            // Garantir que contentType está sempre definido corretamente
            val actualContentType = when {
                detectedMimeType != null && detectedMimeType.startsWith("image/") -> {
                    android.util.Log.d("FeedMediaRepository", "MIME type detectado (imagem): $detectedMimeType para URI: $uri")
                    detectedMimeType
                }
                detectedMimeType != null && detectedMimeType.startsWith("video/") -> {
                    android.util.Log.d("FeedMediaRepository", "MIME type detectado (vídeo): $detectedMimeType para URI: $uri")
                    detectedMimeType
                }
                else -> {
                    // Fallback baseado no tipo de mídia informado e extensão do arquivo
                    val fallbackType = when {
                        extension == "mp4" || extension == "mov" || mediaType == "video" -> "video/mp4"
                        extension == "jpg" || extension == "jpeg" -> "image/jpeg"
                        extension == "png" -> "image/png"
                        extension == "gif" -> "image/gif"
                        extension == "webp" -> "image/webp"
                        else -> "image/jpeg" // Padrão seguro
                    }
                    android.util.Log.w("FeedMediaRepository", "MIME type não detectado ou inválido ($detectedMimeType). Usando fallback baseado em extensão ($extension) e tipo ($mediaType): $fallbackType")
                    fallbackType
                }
            }
            
            val fileSizeDisplay = if (fileSize > 0) "${fileSizeMB}MB" else "desconhecido"
            android.util.Log.d("FeedMediaRepository", "Fazendo upload para: posts/$authenticatedUserId/$fullFileName com contentType: $actualContentType, tamanho: $fileSizeDisplay")
            
            // Criar metadata com contentType garantido
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(actualContentType)
                .build()
            
            // Fazer upload do arquivo com metadata
            android.util.Log.d("FeedMediaRepository", "Iniciando upload...")
            val uploadTask = storageRef.putFile(uri, metadata)
            val uploadSnapshot = uploadTask.await()
            
            // Log informações do upload concluído (usando o snapshot para informações detalhadas)
            android.util.Log.d("FeedMediaRepository", "Upload concluído com sucesso. Bytes transferidos: ${uploadSnapshot.bytesTransferred}, Total bytes: ${uploadSnapshot.totalByteCount}")
            
            // Obter URL de download usando a referência original do storage
            val downloadUrl = storageRef.downloadUrl.await()
            Result.Success(downloadUrl.toString())
        } catch (e: Exception) {
            android.util.Log.e("FeedMediaRepository", "Erro ao fazer upload de mídia: ${e.message}", e)
            android.util.Log.e("FeedMediaRepository", "URI: $uri, userId: $userId, mediaType: $mediaType, fileName: $fullFileName")
            
            // Tratamento detalhado de diferentes tipos de erros do Firebase Storage
            val errorMessage = when {
                // Erro de autenticação
                e.message?.contains("UNAUTHENTICATED", ignoreCase = true) == true ||
                e.message?.contains("authentication", ignoreCase = true) == true -> {
                    android.util.Log.e("FeedMediaRepository", "Erro de autenticação: usuário não autenticado")
                    "Usuário não autenticado. Por favor, faça login novamente."
                }
                // Erro de permissão
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                e.message?.contains("does not have permission", ignoreCase = true) == true ||
                e.message?.contains("Permission denied", ignoreCase = true) == true -> {
                    android.util.Log.e("FeedMediaRepository", "Erro de permissão: verificar regras do Storage e autenticação")
                    "Você não tem permissão para fazer upload deste arquivo. Verifique se está autenticado e tente novamente."
                }
                // Erro de rede/conexão
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    android.util.Log.e("FeedMediaRepository", "Erro de rede: problema de conexão")
                    "Erro de conexão. Verifique sua internet e tente novamente."
                }
                // Erro de tamanho de arquivo
                e.message?.contains("size", ignoreCase = true) == true ||
                e.message?.contains("too large", ignoreCase = true) == true -> {
                    android.util.Log.e("FeedMediaRepository", "Erro de tamanho: arquivo muito grande")
                    "Arquivo muito grande. Limite máximo: 50MB."
                }
                // Erro de formato
                e.message?.contains("contentType", ignoreCase = true) == true ||
                e.message?.contains("format", ignoreCase = true) == true -> {
                    android.util.Log.e("FeedMediaRepository", "Erro de formato: tipo de arquivo não suportado")
                    "Formato de arquivo não suportado. Use imagens (JPG, PNG) ou vídeos (MP4)."
                }
                // Erro genérico
                else -> {
                    android.util.Log.e("FeedMediaRepository", "Erro desconhecido: ${e.message}")
                    "Erro ao fazer upload: ${e.message ?: "Erro desconhecido. Tente novamente."}"
                }
            }
            Result.Error(Exception(errorMessage, e))
        }
    }
    
    /**
     * Faz upload de múltiplas mídias
     */
    suspend fun uploadPostMediaBatch(
        uris: List<Uri>,
        userId: String,
        mediaTypes: List<String>
    ): Result<List<String>> {
        return try {
            if (uris.size != mediaTypes.size) {
                return Result.Error(Exception("Número de URIs e tipos de mídia não correspondem"))
            }
            
            val urls = mutableListOf<String>()
            for (i in uris.indices) {
                val result = uploadPostMedia(uris[i], userId, mediaTypes[i])
                when (result) {
                    is Result.Success -> urls.add(result.data)
                    is Result.Error -> return Result.Error(result.exception)
                    else -> return Result.Error(Exception("Resultado desconhecido"))
                }
            }
            
            Result.Success(urls)
        } catch (e: Exception) {
            android.util.Log.e("FeedMediaRepository", "Erro ao fazer upload em lote: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Deleta uma mídia do Storage
     */
    suspend fun deletePostMedia(mediaUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(mediaUrl)
            storageRef.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FeedMediaRepository", "Erro ao deletar mídia: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Faz upload de mídia de Story para Firebase Storage
     * Segue a mesma lógica de produtos e serviços: sempre usa o userId do usuário autenticado
     */
    suspend fun uploadStoryMedia(
        uri: Uri,
        userId: String,
        mediaType: String
    ): Result<String> {
        return try {
            // CRÍTICO: Sempre obter userId do usuário autenticado (mesma lógica de produtos/serviços)
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                android.util.Log.e("FeedMediaRepository", "Usuário não autenticado")
                return Result.Error(Exception("Usuário não autenticado"))
            }
            
            // Usar sempre o userId do usuário autenticado para garantir permissões corretas
            val authenticatedUserId = currentUser.uid
            
            // Validar que o userId passado corresponde ao autenticado (segurança adicional)
            if (userId != authenticatedUserId) {
                android.util.Log.e("FeedMediaRepository", "UserId passado ($userId) não corresponde ao autenticado ($authenticatedUserId)")
                return Result.Error(Exception("Permissão negada: userId não corresponde ao usuário autenticado"))
            }
            
            val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}"
            val extension = getFileExtension(uri, mediaType)
            val fullFileName = "$fileName.$extension"
            
            android.util.Log.d("FeedMediaRepository", "Upload de story - userId: $authenticatedUserId, path: stories/$authenticatedUserId/$fullFileName")
            
            // Criar referência no Storage: stories/{userId}/{fileName} (mesma estrutura de produtos/serviços)
            val storageRef: StorageReference = storage.reference
                .child(storiesStoragePath)
                .child(authenticatedUserId)
                .child(fullFileName)
            
            // Detectar contentType real do arquivo
            val actualContentType = try {
                context.contentResolver.getType(uri) ?: when (mediaType) {
                    "video" -> "video/mp4"
                    else -> "image/jpeg"
                }
            } catch (e: Exception) {
                when (mediaType) {
                    "video" -> "video/mp4"
                    else -> "image/jpeg"
                }
            }
            
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(actualContentType)
                .build()
            
            val uploadTask = storageRef.putFile(uri, metadata)
            val uploadSnapshot = uploadTask.await()
            
            // Log informações do upload de story concluído (usando o snapshot para informações detalhadas)
            android.util.Log.d("FeedMediaRepository", "Story upload concluído com sucesso. Bytes transferidos: ${uploadSnapshot.bytesTransferred}, Total bytes: ${uploadSnapshot.totalByteCount}")
            
            // Obter URL de download usando a referência original do storage
            val downloadUrl = storageRef.downloadUrl.await()
            Result.Success(downloadUrl.toString())
        } catch (e: Exception) {
            android.util.Log.e("FeedMediaRepository", "Erro ao fazer upload de story: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Obtém a extensão do arquivo baseado no URI e tipo de mídia
     * Usa ContentResolver para detectar MIME type correto
     */
    private fun getFileExtension(uri: Uri, mediaType: String): String {
        // Tentar obter MIME type usando ContentResolver primeiro
        val mimeType = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            android.util.Log.w("FeedMediaRepository", "Erro ao obter MIME type: ${e.message}")
            null
        }
        
        // Se temos MIME type, extrair extensão dele
        if (mimeType != null) {
            return when {
                mimeType.startsWith("image/") -> mimeType.substringAfter("image/")
                mimeType.startsWith("video/") -> mimeType.substringAfter("video/")
                else -> {
                    // Fallback: tentar obter extensão do URI
                    val uriPath = uri.toString()
                    val lastDot = uriPath.lastIndexOf('.')
                    if (lastDot > 0 && lastDot < uriPath.length - 1) {
                        uriPath.substring(lastDot + 1).lowercase()
                    } else {
                        when (mediaType) {
                            "video" -> "mp4"
                            else -> "jpg"
                        }
                    }
                }
            }
        }
        
        // Fallback: tentar obter extensão do URI
        val uriPath = uri.toString()
        val lastDot = uriPath.lastIndexOf('.')
        if (lastDot > 0 && lastDot < uriPath.length - 1) {
            val extension = uriPath.substring(lastDot + 1).lowercase()
            if (extension.isNotEmpty() && (extension == "jpg" || extension == "jpeg" || extension == "png" || extension == "mp4" || extension == "mov")) {
                return extension
            }
        }
        
        // Último fallback baseado no tipo de mídia
        return when (mediaType) {
            "video" -> "mp4"
            "image" -> "jpg"
            else -> "jpg"
        }
    }
}
