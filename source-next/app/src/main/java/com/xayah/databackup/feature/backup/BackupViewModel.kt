package com.xayah.databackup.feature.backup

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Statistics(
    val selectedCount: Int = 0,
    val totalCount: Int = 0,
    val selectedSize: Long = 0,
    val isAnySelected: Boolean = false,
)

open class BackupViewModel : BaseViewModel() {
    val appsStatistics: StateFlow<Statistics> = DatabaseHelper.appDao.loadFlowApps().map { apps ->
        val selectedCount = apps.count { it.toggleableState != ToggleableState.Off }
        Statistics(
            selectedCount = selectedCount,
            totalCount = apps.size,
            selectedSize = apps.sumOf { it.selectedBytes },
            isAnySelected = selectedCount > 0,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = Statistics(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun selectAllApps(selected: Boolean) {
        withLock(Dispatchers.IO) {
            val apps = DatabaseHelper.appDao.loadFlowApps().first()
            val groupedApps = apps.groupBy { it.userId }
            groupedApps.forEach { (userId, userApps) ->
                val packageNames = userApps.map { it.packageName }
                DatabaseHelper.appDao.selectAll(packageNames = packageNames, userId = userId, selected = selected)
            }
        }
    }
}
