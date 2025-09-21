package com.example.taskgo.backend.routes

import com.example.taskgo.backend.realtime.EventBus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.collect

fun Route.realtimeRoutes() {
    get("/events") {
        call.response.cacheControl(CacheControl.NoCache(null))
        call.response.headers.append(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
        val topicsParam = call.request.queryParameters.getOrFail("topics")
        val topics = topicsParam.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (topics.isEmpty()) { call.respond(HttpStatusCode.BadRequest, "no topics"); return@get }

        call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
            EventBus.subscribe(*topics.toTypedArray()).collect { msg ->
                val data = "data: ${msg.replace("\n", " ")}\n\n"
                writeStringUtf8(data)
                flush()
            }
        }
    }
}


