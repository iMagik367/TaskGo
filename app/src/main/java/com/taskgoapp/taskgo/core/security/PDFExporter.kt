package com.taskgoapp.taskgo.core.security

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDFExporter @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PDFExporter"
    }
    
    /**
     * Exporta dados do usuário em formato PDF e salva no dispositivo
     */
    suspend fun exportUserDataToPDF(
        userId: String,
        userData: Map<String, Any>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Criar diretório para downloads
            val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TaskGo")
            }
            
            downloadsDir?.mkdirs()
            
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "TaskGo_Dados_${userId}_${dateFormat.format(Date())}.pdf"
            val file = File(downloadsDir, fileName)
            
            // Criar PDF
            val writer = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)
            
            // Título
            document.add(
                Paragraph("Relatório de Dados do Usuário - TaskGo")
                    .setFontSize(20f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            document.add(
                Paragraph("Data de Exportação: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30f)
            )
            
            // Dados do Perfil
            val profileData = userData["profile"] as? Map<*, *>
            if (profileData != null) {
                document.add(
                    Paragraph("Dados do Perfil")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                
                val profileTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                profileTable.setWidth(UnitValue.createPercentValue(100f))
                
                profileData.forEach { (key, value) ->
                    if (value != null && value.toString().isNotBlank()) {
                        profileTable.addCell(Paragraph(key.toString()).setBold())
                        profileTable.addCell(Paragraph(value.toString()))
                    }
                }
                
                document.add(profileTable)
            }
            
            // Pedidos
            val orders = userData["orders"] as? List<*>
            if (orders != null && orders.isNotEmpty()) {
                document.add(
                    Paragraph("Pedidos (${orders.size})")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                
                orders.forEachIndexed { index, order ->
                    val orderMap = order as? Map<*, *>
                    if (orderMap != null) {
                        document.add(
                            Paragraph("Pedido ${index + 1}")
                                .setFontSize(14f)
                                .setBold()
                                .setMarginTop(10f)
                        )
                        
                        val orderTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                        orderTable.setWidth(UnitValue.createPercentValue(100f))
                        
                        orderMap.forEach { (key, value) ->
                            if (value != null && value.toString().isNotBlank()) {
                                orderTable.addCell(Paragraph(key.toString()).setBold())
                                orderTable.addCell(Paragraph(value.toString()))
                            }
                        }
                        
                        document.add(orderTable)
                    }
                }
            }
            
            // Avaliações
            val reviews = userData["reviews"] as? List<*>
            if (reviews != null && reviews.isNotEmpty()) {
                document.add(
                    Paragraph("Avaliações (${reviews.size})")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                
                reviews.forEachIndexed { index, review ->
                    val reviewMap = review as? Map<*, *>
                    if (reviewMap != null) {
                        document.add(
                            Paragraph("Avaliação ${index + 1}")
                                .setFontSize(14f)
                                .setBold()
                                .setMarginTop(10f)
                        )
                        
                        val reviewTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                        reviewTable.setWidth(UnitValue.createPercentValue(100f))
                        
                        reviewMap.forEach { (key, value) ->
                            if (value != null && value.toString().isNotBlank()) {
                                reviewTable.addCell(Paragraph(key.toString()).setBold())
                                reviewTable.addCell(Paragraph(value.toString()))
                            }
                        }
                        
                        document.add(reviewTable)
                    }
                }
            }
            
            // Notificações
            val notifications = userData["notifications"] as? List<*>
            if (notifications != null && notifications.isNotEmpty()) {
                document.add(
                    Paragraph("Notificações (${notifications.size})")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f)
                )
                
                notifications.take(50).forEachIndexed { index, notification ->
                    val notifMap = notification as? Map<*, *>
                    if (notifMap != null) {
                        document.add(
                            Paragraph("Notificação ${index + 1}")
                                .setFontSize(14f)
                                .setBold()
                                .setMarginTop(10f)
                        )
                        
                        val notifTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
                        notifTable.setWidth(UnitValue.createPercentValue(100f))
                        
                        notifMap.forEach { (key, value) ->
                            if (value != null && value.toString().isNotBlank()) {
                                notifTable.addCell(Paragraph(key.toString()).setBold())
                                notifTable.addCell(Paragraph(value.toString()))
                            }
                        }
                        
                        document.add(notifTable)
                    }
                }
            }
            
            // Rodapé
            document.add(
                Paragraph("\n\nEste documento contém todos os dados pessoais associados à sua conta no TaskGo.")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30f)
            )
            
            document.close()
            
            Log.d(TAG, "PDF exportado com sucesso: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao exportar PDF: ${e.message}", e)
            Result.failure(e)
        }
    }
}

