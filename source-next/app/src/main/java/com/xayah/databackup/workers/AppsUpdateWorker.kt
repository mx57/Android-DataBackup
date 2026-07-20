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
                val activeUsers = RemoteRootService.getUsers()
                val activeUserIds = activeUsers.map { it.id }.toSet()

                // Get user IDs from local DB and delete inactive users
                val localApps = DatabaseHelper.appDao.loadFlowApps().first()
                val localUserIds = localApps.map { it.userId }.toSet()
                val inactiveUserIds = localUserIds - activeUserIds
                inactiveUserIds.forEach { userId ->
                    DatabaseHelper.appDao.deleteByUserId(userId)
                }

                val apps = RemoteRootService.getInstalledApps()
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
