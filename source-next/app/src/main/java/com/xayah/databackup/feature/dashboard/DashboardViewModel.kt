package com.xayah.databackup.feature.dashboard

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BackupDir
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.readString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val backupsBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val isLoading: Boolean = true,
)

class DashboardViewModel : BaseViewModel() {
    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    init {
        viewModelScope.launch {
            App.application.readString(BackupDir).collectLatest { backupDir ->
                updateStats(backupDir)
            }
        }
    }

    private suspend fun updateStats(backupDir: String) {
        _storageStats.update { it.copy(isLoading = true) }
        withContext(Dispatchers.IO) {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val backupsBytes = RemoteRootService.getDirSize(backupDir)
            val otherBytes = (totalBytes - freeBytes - backupsBytes).coerceAtLeast(0L)

            _storageStats.update {
                it.copy(
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    backupsBytes = backupsBytes,
                    otherBytes = otherBytes,
                    isLoading = false
                )
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val backupDir = App.application.readString(BackupDir).first()
            updateStats(backupDir)
        }
    }
}
