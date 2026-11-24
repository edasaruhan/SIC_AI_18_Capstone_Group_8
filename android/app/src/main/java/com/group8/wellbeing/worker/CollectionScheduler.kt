package com.group8.wellbeing.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.group8.wellbeing.data.AppConfig
import java.util.concurrent.TimeUnit

interface CollectionScheduler {
    fun schedulePeriodicCollection()
    fun cancelPeriodicCollection()
    fun isScheduled(): Boolean
}

class WorkManagerCollectionScheduler(context: Context) : CollectionScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    @Volatile
    private var scheduledFlag: Boolean = false

    override fun schedulePeriodicCollection() {
        val request = PeriodicWorkRequestBuilder<UsageCollectionWorker>(
            AppConfig.WORK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        scheduledFlag = true
    }

    override fun cancelPeriodicCollection() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        scheduledFlag = false
    }

    override fun isScheduled(): Boolean = scheduledFlag

    companion object {
        const val UNIQUE_WORK_NAME: String = "wellbeing_periodic_usage_collection"
    }
}
