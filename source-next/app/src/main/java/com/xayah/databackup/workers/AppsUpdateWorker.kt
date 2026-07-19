package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsUpdateWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private var mNotificationBuilder = NotificationHelper.getNotificationBuilder(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationHelper.generateNotificationId(), mNotificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationHelper.generateNotificationId(), mNotificationBuilder.build())
        }
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        withContext(Dispatchers.Default) {
            runCatching {
                val apps = RemoteRootService.getInstalledApps()
                val users = RemoteRootService.getUsers()
                val activeUserIds = users.map { it.id }.toSet()

                if (activeUserIds.isNotEmpty()) {
                    // 1. Delete apps for any user IDs currently in the database but no longer active on the system
                    val dbUserIds = DatabaseHelper.appDao.getDistinctUserIds()
                    dbUserIds.forEach { dbUserId ->
                        if (dbUserId !in activeUserIds) {
                            DatabaseHelper.appDao.deleteByUserId(dbUserId)
                        }
                    }

                    // 2. For active users, group apps and delete uninstalled apps
                    val groupedApps = apps.groupBy { it.userId }
                    activeUserIds.forEach { userId ->
                        val userApps = groupedApps[userId] ?: emptyList()
                        val packageNames = userApps.map { it.packageName }
                        if (packageNames.isEmpty()) {
                            DatabaseHelper.appDao.deleteByUserId(userId)
                        } else {
                            DatabaseHelper.appDao.deleteExcept(packageNames, userId)
                        }
                    }
                }

                // 3. Upsert the updated apps
                DatabaseHelper.appDao.upsertParcelable(apps)
            }.onFailure {
                LogHelper.e(TAG, "Failed to update apps.", it)
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "AppsUpdateWorker"

        fun buildRequest() = OneTimeWorkRequestBuilder<AppsUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
