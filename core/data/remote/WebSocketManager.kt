package com.example.taskgoapp.core.data.remote

import android.util.Log
import com.example.taskgoapp.core.model.Report
import com.example.taskgoapp.core.model.Service
import com.example.taskgoapp.core.model.User
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor() {
    private var socket: Socket? = null
    private val json = Json { ignoreUnknownKeys = true }

    // Canais para diferentes tipos de eventos
    private val serviceUpdates = Channel<Service>()
    private val userUpdates = Channel<User>()
    private val moderationUpdates = Channel<Report>()
    private val connectionStatus = Channel<Boolean>()

    fun connect(serverUrl: String, token: String) {
        try {
            val options = IO.Options().apply {
                auth = mapOf("token" to token)
                transports = arrayOf("websocket")
                timeout = 10000 // 10 segundos
                reconnection = true
                reconnectionAttempts = 3
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
            }

            socket = IO.socket(serverUrl, options).apply {
                // Eventos de conexão
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket conectado")
                    connectionStatus.trySend(true)
                }
                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Socket desconectado")
                    connectionStatus.trySend(false)
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "Erro de conexão: ${args[0]}")
                    connectionStatus.trySend(false)
                }

                // Eventos de serviço
                on("serviceUpdated") { args ->
                    try {
                        val serviceJson = (args[0] as JSONObject).toString()
                        val service = json.decodeFromString<Service>(serviceJson)
                        serviceUpdates.trySend(service)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar atualização de serviço", e)
                    }
                }

                // Eventos de usuário
                on("userUpdated") { args ->
                    try {
                        val userJson = (args[0] as JSONObject).toString()
                        val user = json.decodeFromString<User>(userJson)
                        userUpdates.trySend(user)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar atualização de usuário", e)
                    }
                }

                // Eventos de moderação
                on("moderationAction") { args ->
                    try {
                        val reportJson = (args[0] as JSONObject).toString()
                        val report = json.decodeFromString<Report>(reportJson)
                        moderationUpdates.trySend(report)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar ação de moderação", e)
                    }
                }

                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar socket", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun watchService(serviceId: String) {
        socket?.emit("watchService", serviceId)
    }

    fun unwatchService(serviceId: String) {
        socket?.emit("unwatchService", serviceId)
    }

    fun getServiceUpdates(): Flow<Service> = serviceUpdates.receiveAsFlow()
    fun getUserUpdates(): Flow<User> = userUpdates.receiveAsFlow()
    fun getModerationUpdates(): Flow<Report> = moderationUpdates.receiveAsFlow()
    fun getConnectionStatus(): Flow<Boolean> = connectionStatus.receiveAsFlow()

    companion object {
        private const val TAG = "WebSocketManager"
    }
}