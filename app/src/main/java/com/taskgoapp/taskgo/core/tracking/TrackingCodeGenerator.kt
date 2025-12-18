package com.taskgoapp.taskgo.core.tracking

import kotlin.math.absoluteValue

object TrackingCodeGenerator {
    fun generate(orderId: String): String {
        val normalized = orderId.filter { it.isLetterOrDigit() }
        val hash = normalized.hashCode().absoluteValue % 1_000_000_000
        val middle = hash.toString().padStart(9, '0')
        return "TG${middle}BR"
    }
}

