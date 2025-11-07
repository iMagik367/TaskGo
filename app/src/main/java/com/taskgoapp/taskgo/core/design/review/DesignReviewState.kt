package com.taskgoapp.taskgo.core.design.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DesignReviewState {
    var overlayEnabled by mutableStateOf(false)
    var overlayAlpha by mutableStateOf(0.5f)
    var overlayScreenshotRes by mutableStateOf<Int?>(null)
    var gridEnabled by mutableStateOf(false)
}




