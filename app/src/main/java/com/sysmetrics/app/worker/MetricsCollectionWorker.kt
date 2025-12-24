package com.sysmetrics.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sysmetrics.app.domain.repository.IMetricsHistoryRepository
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background metrics collection.
 * Collects system metrics periodically and stores them in the database.
 */
@HiltWorker
class MetricsCollectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val systemMetricsRepository: ISystemMetricsRepository,
    private val historyRepository: IMetricsHistoryRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MetricsWorker"
        const val WORK_NAME = "metrics_collection_work"
        private const val DEFAULT_INTERVAL_MINUTES = 1L

        /**
         * Schedule periodic metrics collection.
         */
        fun schedule(
            context: Context,
            intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES
        ) {
            val workRequest = PeriodicWorkRequestBuilder<MetricsCollectionWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Timber.tag(TAG).i("Scheduled metrics collection every $intervalMinutes minutes")
        }

        /**
         * Cancel scheduled metrics collection.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.tag(TAG).i("Cancelled metrics collection")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.tag(TAG).d("Starting background metrics collection")
            
            // Collect current metrics
            val metrics = systemMetricsRepository.collectMetrics()
            
            // Save to history
            historyRepository.saveMetrics(metrics)
            
            // Cleanup old entries (older than 24 hours)
            val deleted = historyRepository.cleanupOldEntries()
            if (deleted > 0) {
                Timber.tag(TAG).d("Cleaned up $deleted old entries")
            }
            
            Timber.tag(TAG).d("Background collection completed: CPU=${metrics.cpuUsage}%, RAM=${metrics.ramUsagePercent}%")
            
            Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Background collection failed")
            Result.retry()
        }
    }
}
