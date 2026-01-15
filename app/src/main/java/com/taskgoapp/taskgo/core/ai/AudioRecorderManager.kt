package com.taskgoapp.taskgo.core.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    
    /**
     * Inicia a gravação de áudio
     * Retorna o arquivo de saída onde o áudio será gravado
     */
    suspend fun startRecording(): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (isRecording) {
                return@withContext Result.failure(Exception("Já está gravando"))
            }
            
            // Criar arquivo temporário para o áudio
            // IMPORTANTE: Usar formato LINEAR16 (WAV PCM) para melhor compatibilidade com Google Speech-to-Text API
            // O formato M4A/AAC pode causar problemas de encoding na API
            // Para Android Q+, usar formato que pode ser convertido para LINEAR16
            outputFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Tentar usar formato mais compatível (WAV se possível, senão M4A)
                try {
                    File.createTempFile("audio_recording_", ".wav", context.cacheDir)
                } catch (e: Exception) {
                    // Fallback para M4A se WAV não for suportado
                    File.createTempFile("audio_recording_", ".m4a", context.cacheDir)
                }
            } else {
                File.createTempFile("audio_recording_", ".3gp", context.cacheDir)
            }
            val file = outputFile ?: return@withContext Result.failure(Exception("Não foi possível criar arquivo"))
            
            // Criar MediaRecorder baseado na versão do Android
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            val recorder = mediaRecorder ?: return@withContext Result.failure(Exception("Não foi possível criar MediaRecorder"))
            
            // Configurar MediaRecorder para melhor qualidade e compatibilidade com Speech-to-Text
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            
            // Usar formato apropriado baseado na versão do Android e extensão do arquivo
            val fileName = file.name.lowercase()
            if (fileName.endsWith(".wav") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Tentar usar formato WAV (LINEAR16) se suportado
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // Fallback, WAV não é suportado diretamente
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    }
                } catch (e: Exception) {
                    // Se WAV falhar, usar M4A
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android Q+: Usar M4A/AAC com configurações otimizadas para Speech-to-Text
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(16000) // Taxa de amostragem padrão para Speech-to-Text (16kHz)
            } else {
                // Android antigo: Usar 3GP/AMR
                @Suppress("DEPRECATION")
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                @Suppress("DEPRECATION")
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                recorder.setAudioEncodingBitRate(12200)
                recorder.setAudioSamplingRate(8000) // Taxa de amostragem para AMR (8kHz)
            }
            
            recorder.setOutputFile(file.absolutePath)
            
            // Preparar e iniciar gravação
            recorder.prepare()
            recorder.start()
            
            isRecording = true
            
            Result.success(file)
        } catch (e: Exception) {
            stopRecording()
            Result.failure(e)
        }
    }
    
    /**
     * Para a gravação de áudio
     * Retorna o arquivo gravado ou null se não houver gravação
     */
    suspend fun stopRecording(): Result<File?> = withContext(Dispatchers.IO) {
        try {
            if (!isRecording) {
                return@withContext Result.success(null)
            }
            
            val recorder = mediaRecorder
            val file = outputFile
            
            recorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    // Ignorar erros ao parar (pode ocorrer se não houver dados suficientes)
                }
            }
            
            mediaRecorder = null
            isRecording = false
            
            Result.success(file)
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Result.failure(e)
        }
    }
    
    /**
     * Cancela a gravação atual e remove o arquivo
     */
    suspend fun cancelRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stopRecording()
            outputFile?.delete()
            outputFile = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retorna o status atual da gravação
     */
    fun isCurrentlyRecording(): Boolean = isRecording
    
    /**
     * Obtém a amplitude atual do áudio (0.0 a 1.0) para animação visual
     * Retorna 0.0 se não estiver gravando
     */
    fun getAmplitude(): Double {
        return if (isRecording && mediaRecorder != null) {
            try {
                val maxAmplitude = mediaRecorder?.maxAmplitude ?: 0
                // Normalizar para 0.0 a 1.0
                (maxAmplitude / 32767.0).coerceIn(0.0, 1.0)
            } catch (e: Exception) {
                0.0
            }
        } else {
            0.0
        }
    }
}
