package com.xayah.databackup.feature.backup

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Statistics(
    val selectedCount: Int = 0,
    val totalCount: Int = 0,
    val selectedSize: Long = 0,
)

open class BackupViewModel : BaseViewModel() {
    val appsStatistics: StateFlow<Statistics> = DatabaseHelper.appDao.loadFlowApps().map { apps ->
        Statistics(
            selectedCount = apps.count { it.toggleableState != ToggleableState.Off },
            totalCount = apps.size,
            selectedSize = apps.sumOf { it.selectedBytes }
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = Statistics(),
        started = SharingStarted.WhileSubscribed(5_000),
    )
}
