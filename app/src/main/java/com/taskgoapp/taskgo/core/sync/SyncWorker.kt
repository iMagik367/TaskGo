package com.taskgoapp.taskgo.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskgoapp.taskgo.data.local.dao.SyncQueueDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("SyncWorker", "Executando ciclo de sincronização")
            syncManager.runOneSyncCycle()
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Erro na sincronização periódica: ${e.message}", e)
            Result.retry()
        }
    }
}


