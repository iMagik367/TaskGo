package com.example.taskgo.backend.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.merge

object EventBus {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val topics: MutableMap<String, MutableSharedFlow<String>> = mutableMapOf()

    private fun topic(name: String): MutableSharedFlow<String> =
        topics.getOrPut(name) { MutableSharedFlow(extraBufferCapacity = 128, replay = 0) }

    suspend fun publish(name: String, message: String) {
        topic(name).emit(message)
    }

    fun subscribe(vararg names: String): Flow<String> {
        val flows = names.map { topic(it).asSharedFlow() }
        return merge(*flows.toTypedArray())
    }
}


