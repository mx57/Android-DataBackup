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
import kotlinx.coroutines.flow.first
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
                val activeUsers = RemoteRootService.getUsers()
                val activeUserIds = activeUsers.map { it.id }.toSet()

                // Clean up inactive users' databases
                if (activeUserIds.isNotEmpty()) {
                    DatabaseHelper.appDao.deleteExceptUserIds(activeUserIds.toList())
                }

                // Clean up active users' databases
                val groupedApps = apps.groupBy { it.userId }
                activeUserIds.forEach { userId ->
                    val userApps = groupedApps[userId] ?: emptyList()
                    if (userApps.isNotEmpty()) {
                        val packageNames = userApps.map { it.packageName }
                        DatabaseHelper.appDao.deleteExcept(packageNames, userId)
                    } else {
                        DatabaseHelper.appDao.deleteByUserId(userId)
                    }
                }

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
