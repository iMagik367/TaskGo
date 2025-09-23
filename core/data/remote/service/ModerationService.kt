package com.example.taskgoapp.core.data.remote.service

import com.example.taskgoapp.core.data.remote.model.ApiResponse
import com.example.taskgoapp.core.model.Report
import com.example.taskgoapp.core.model.ReportType
import retrofit2.http.*

interface ModerationService {
    @POST("moderation/reports")
    suspend fun createReport(
        @Query("type") type: ReportType,
        @Query("description") description: String,
        @Query("contentId") contentId: String,
        @Query("contentType") contentType: String
    ): ApiResponse<Report>

    @GET("moderation/reports/me")
    suspend fun getMyReports(): ApiResponse<List<Report>>

    @PUT("moderation/documents/{documentId}")
    suspend fun updateDocument(
        @Path("documentId") documentId: String,
        @Query("url") documentUrl: String
    ): ApiResponse<Unit>

    @DELETE("moderation/reports/{reportId}")
    suspend fun deleteReport(
        @Path("reportId") reportId: String
    ): ApiResponse<Unit>
}