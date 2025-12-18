package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.MessageDao
import com.taskgoapp.taskgo.data.mapper.MessageMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.MessageMapper.toModel
import com.taskgoapp.taskgo.domain.repository.MessageRepository
import com.taskgoapp.taskgo.core.model.MessageThread
import com.taskgoapp.taskgo.core.model.ChatMessage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val database: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : MessageRepository {
    
    private val rootRef: DatabaseReference = database.reference
    private val messagesRef: DatabaseReference = rootRef.child("messages")
    private val threadsRef: DatabaseReference = rootRef.child("conversations")
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun observeThreads(): Flow<List<MessageThread>> = callbackFlow {
        val currentUserId = firebaseAuth.currentUser?.uid
        
        if (currentUserId == null) {
            // Se não autenticado, retornar apenas cache local
            val cachedThreads = messageDao.observeThreads().map { entities ->
                entities.map { it.toModel() }
            }
            cachedThreads.collect { threads ->
                trySend(threads)
            }
            awaitClose { }
            return@callbackFlow
        }
        
        // Observar Firebase Realtime Database
        val listener = threadsRef
            .orderByChild("participants/$currentUserId")
            .equalTo(true)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val threads = mutableListOf<MessageThread>()
                    snapshot.children.forEach { threadSnapshot ->
                        val threadData = threadSnapshot.getValue(Map::class.java) as? Map<*, *>
                        threadData?.let { data ->
                            val thread = MessageThread(
                                id = threadSnapshot.key ?: "",
                                title = data["title"] as? String ?: "",
                                lastMessage = data["lastMessage"] as? String ?: "",
                                lastTime = (data["lastTime"] as? Long) ?: 0L
                            )
                            threads.add(thread)
                            // Salvar no cache local em background
                            syncScope.launch {
                                messageDao.upsertThread(thread.toEntity())
                            }
                        }
                    }
                    trySend(threads.sortedByDescending { it.lastTime })
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("MessageRepo", "Erro ao observar threads: ${error.message}")
                    // Em caso de erro, tentar retornar cache local
                    syncScope.launch {
                        val cachedThreads = messageDao.observeThreads().map { entities ->
                            entities.map { it.toModel() }
                        }
                        cachedThreads.collect { threads ->
                            trySend(threads)
                        }
                    }
                }
            })
        
        awaitClose { threadsRef.removeEventListener(listener) }
    }

    override suspend fun getThread(id: String): MessageThread? {
        // 1. Tentar buscar do cache local primeiro
        val cached = messageDao.getThreadById(id)?.toModel()
        if (cached != null) return cached
        
        // 2. Buscar do Firebase
        return try {
            val snapshot = threadsRef.child(id).get().await()
            val data = snapshot.getValue(Map::class.java) as? Map<*, *>
            data?.let {
                val thread = MessageThread(
                    id = id,
                    title = it["title"] as? String ?: "",
                    lastMessage = it["lastMessage"] as? String ?: "",
                    lastTime = (it["lastTime"] as? Long) ?: 0L
                )
                // Salvar no cache
                messageDao.upsertThread(thread.toEntity())
                thread
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "Erro ao buscar thread: ${e.message}", e)
            null
        }
    }

    override fun observeMessages(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = firebaseAuth.currentUser?.uid
        
        if (currentUserId == null) {
            // Se não autenticado, retornar apenas cache local
            val cachedMessages = messageDao.observeMessages(threadId).map { entities ->
                entities.map { it.toModel() }
            }
            cachedMessages.collect { messages ->
                trySend(messages.sortedBy { it.time })
            }
            awaitClose { }
            return@callbackFlow
        }
        
        // Observar Firebase Realtime Database
        val listener = messagesRef
            .child(threadId)
            .orderByChild("time")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val messages = mutableListOf<ChatMessage>()
                    snapshot.children.forEach { messageSnapshot ->
                        val messageData = messageSnapshot.getValue(Map::class.java) as? Map<*, *>
                        messageData?.let { data ->
                            val senderId = data["senderId"] as? String ?: ""
                            val message = ChatMessage(
                                id = messageSnapshot.key ?: "",
                                threadId = threadId,
                                senderMe = senderId == currentUserId,
                                text = data["text"] as? String ?: "",
                                time = (data["time"] as? Long) ?: 0L
                            )
                            messages.add(message)
                            // Salvar no cache local em background
                            syncScope.launch {
                                messageDao.upsertMessage(message.toEntity())
                            }
                        }
                    }
                    trySend(messages.sortedBy { it.time })
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("MessageRepo", "Erro ao observar mensagens: ${error.message}")
                    // Em caso de erro, tentar retornar cache local
                    syncScope.launch {
                        val cachedMessages = messageDao.observeMessages(threadId).map { entities ->
                            entities.map { it.toModel() }
                        }
                        cachedMessages.collect { messages ->
                            trySend(messages.sortedBy { it.time })
                        }
                    }
                }
            })
        
        awaitClose { messagesRef.child(threadId).removeEventListener(listener) }
    }

    override suspend fun sendMessage(threadId: String, text: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Usuário não autenticado")
        
        val messageId = generateMessageId()
        val timestamp = System.currentTimeMillis()
        
        val message = ChatMessage(
            id = messageId,
            threadId = threadId,
            senderMe = true,
            text = text,
            time = timestamp
        )
        
        // 1. Salvar no cache local primeiro (otimista)
        messageDao.upsertMessage(message.toEntity())
        
        // 2. Salvar no Firebase Realtime Database
        try {
            val messageData = mapOf(
                "senderId" to currentUserId,
                "text" to text,
                "time" to timestamp
            )
            messagesRef.child(threadId).child(messageId).setValue(messageData).await()
            
            // 3. Atualizar thread com última mensagem
            val threadUpdate = mapOf(
                "lastMessage" to text,
                "lastTime" to timestamp
            )
            threadsRef.child(threadId).updateChildren(threadUpdate).await()
            
            // 4. Atualizar cache local da thread
            val thread = messageDao.getThreadById(threadId)
            if (thread != null) {
                val updatedThread = thread.copy(
                    lastMessage = text,
                    lastTime = timestamp
                )
                messageDao.upsertThread(updatedThread)
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "Erro ao enviar mensagem: ${e.message}", e)
            throw e
        }
    }

    override suspend fun createThread(title: String): String {
        val currentUserId = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Usuário não autenticado")
        
        val threadId = generateMessageId()
        val timestamp = System.currentTimeMillis()
        
        val thread = MessageThread(
            id = threadId,
            title = title,
            lastMessage = "",
            lastTime = timestamp
        )
        
        // 1. Salvar no cache local primeiro
        messageDao.upsertThread(thread.toEntity())
        
        // 2. Salvar no Firebase Realtime Database
        try {
            val threadData = mapOf(
                "title" to title,
                "lastMessage" to "",
                "lastTime" to timestamp,
                "participants" to mapOf(currentUserId to true),
                "createdAt" to timestamp
            )
            threadsRef.child(threadId).setValue(threadData).await()
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "Erro ao criar thread: ${e.message}", e)
            throw e
        }
        
        return threadId
    }
    
    /**
     * Cria uma thread entre dois usuários (para conversas de pedidos/propostas)
     */
    suspend fun createThreadBetweenUsers(userId1: String, userId2: String, title: String): String {
        val threadId = generateMessageId()
        val timestamp = System.currentTimeMillis()
        
        val thread = MessageThread(
            id = threadId,
            title = title,
            lastMessage = "",
            lastTime = timestamp
        )
        
        // 1. Salvar no cache local
        messageDao.upsertThread(thread.toEntity())
        
        // 2. Salvar no Firebase
        try {
            val threadData = mapOf(
                "title" to title,
                "lastMessage" to "",
                "lastTime" to timestamp,
                "participants" to mapOf(
                    userId1 to true,
                    userId2 to true
                ),
                "createdAt" to timestamp
            )
            threadsRef.child(threadId).setValue(threadData).await()
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "Erro ao criar thread entre usuários: ${e.message}", e)
            throw e
        }
        
        return threadId
    }
    
    /**
     * Busca ou cria uma thread baseada em orderId
     * Retorna o ID da thread existente ou cria uma nova
     */
    suspend fun getOrCreateThreadForOrder(
        orderId: String,
        orderRepository: com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository,
        userRepository: com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
    ): String {
        val currentUserId = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("Usuário não autenticado")
        
        // 1. Buscar ordem para obter clientId e providerId
        val order = orderRepository.getOrder(orderId) ?: throw IllegalArgumentException("Ordem não encontrada: $orderId")
        
        val otherUserId = if (order.clientId == currentUserId) {
            order.providerId ?: throw IllegalArgumentException("Ordem sem prestador")
        } else {
            order.clientId
        }
        
        // 2. Buscar nome do outro usuário
        val otherUser = userRepository.getUser(otherUserId)
        val threadTitle = otherUser?.displayName ?: "Conversa"
        
        // 3. Buscar thread existente entre os dois usuários
        val existingThread = try {
            val snapshot = threadsRef
                .orderByChild("participants/$currentUserId")
                .equalTo(true)
                .get()
                .await()
            
            snapshot.children.firstOrNull { threadSnapshot ->
                val threadData = threadSnapshot.getValue(Map::class.java) as? Map<*, *>
                val participants = threadData?.get("participants") as? Map<*, *>
                participants?.containsKey(otherUserId) == true && 
                (threadData["orderId"] as? String) == orderId
            }?.key
        } catch (e: Exception) {
            null
        }
        
        // 4. Se encontrou thread existente, retornar
        if (existingThread != null) {
            return existingThread
        }
        
        // 5. Criar nova thread
        return createThreadBetweenUsers(
            userId1 = currentUserId,
            userId2 = otherUserId,
            title = threadTitle
        ).also { threadId ->
            // Adicionar orderId à thread
            try {
                threadsRef.child(threadId).updateChildren(mapOf("orderId" to orderId)).await()
            } catch (e: Exception) {
                android.util.Log.w("MessageRepo", "Erro ao adicionar orderId à thread: ${e.message}")
            }
        }
    }
    
    /**
     * Busca ou cria uma thread baseada em providerId
     * Retorna o ID da thread existente ou cria uma nova
     */
    suspend fun getOrCreateThreadForProvider(
        providerId: String,
        userRepository: com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
    ): String {
        val currentUserId = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("Usuário não autenticado")
        
        if (providerId == currentUserId) {
            throw IllegalArgumentException("Não é possível criar thread consigo mesmo")
        }
        
        // 1. Buscar nome do prestador
        val provider = userRepository.getUser(providerId)
        val threadTitle = provider?.displayName ?: "Conversa"
        
        // 2. Buscar thread existente entre os dois usuários
        val existingThread = try {
            val snapshot = threadsRef
                .orderByChild("participants/$currentUserId")
                .equalTo(true)
                .get()
                .await()
            
            snapshot.children.firstOrNull { threadSnapshot ->
                val threadData = threadSnapshot.getValue(Map::class.java) as? Map<*, *>
                val participants = threadData?.get("participants") as? Map<*, *>
                participants?.containsKey(providerId) == true
            }?.key
        } catch (e: Exception) {
            null
        }
        
        // 3. Se encontrou thread existente, retornar
        if (existingThread != null) {
            return existingThread
        }
        
        // 4. Criar nova thread
        return createThreadBetweenUsers(
            userId1 = currentUserId,
            userId2 = providerId,
            title = threadTitle
        )
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}
