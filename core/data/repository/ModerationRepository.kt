package com.example.taskgoapp.core.data.repository

import com.example.taskgoapp.core.data.remote.model.ApiResponse
import com.example.taskgoapp.core.data.remote.service.ModerationService
import com.example.taskgoapp.core.model.Report
import com.example.taskgoapp.core.model.ReportType
import com.example.taskgoapp.core.model.ReportedContent
import javax.inject.Inject

class ModerationRepository @Inject constructor(
    private val moderationService: ModerationService
) {
    suspend fun createReport(
        type: ReportType,
        description: String,
        content: ReportedContent
    ): ApiResponse<Report> {
        return moderationService.createReport(
            type = type,
            description = description,
            contentId = content.id,
            contentType = content.type
        )
    }

    suspend fun getMyReports(): ApiResponse<List<Report>> {
        return moderationService.getMyReports()
    }

    suspend fun updateDocument(
        documentId: String,
        documentUrl: String
    ): ApiResponse<Unit> {
        return moderationService.updateDocument(documentId, documentUrl)
    }

    suspend fun deleteReport(reportId: String): ApiResponse<Unit> {
        return moderationService.deleteReport(reportId)
    }
}