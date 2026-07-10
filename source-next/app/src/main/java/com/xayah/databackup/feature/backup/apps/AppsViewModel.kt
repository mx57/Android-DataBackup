package com.xayah.databackup.feature.backup.apps

import android.content.pm.UserInfo
import androidx.compose.ui.state.ToggleableState
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.feature.backup.Statistics
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.FilterBackupUser
import com.xayah.databackup.util.FiltersSystemAppsBackup
import com.xayah.databackup.util.FiltersUserAppsBackup
import com.xayah.databackup.util.KeyFilterBackupUser
import com.xayah.databackup.util.KeySortsSequenceBackup
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.SortsSequenceBackup
import com.xayah.databackup.util.SortsType
import com.xayah.databackup.util.SortsTypeBackup
import com.xayah.databackup.util.combine
import com.xayah.databackup.util.filter
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.readInt
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.saveBoolean
import com.xayah.databackup.util.saveEnum
import com.xayah.databackup.util.saveInt
import com.xayah.databackup.util.sortByA2Z
import com.xayah.databackup.util.sortByDataSize
import com.xayah.databackup.util.sortByInstallTime
import com.xayah.databackup.util.sortByUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class UiState(
    val searchQuery: String = "",
)

open class AppsViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val apps = combine(
        DatabaseHelper.appDao.loadFlowApps(),
        App.application.readInt(FilterBackupUser),
        App.application.readEnum(SortsTypeBackup),
        App.application.readEnum(SortsSequenceBackup),
        App.application.readBoolean(FiltersUserAppsBackup),
        App.application.readBoolean(FiltersSystemAppsBackup),
        _uiState.map { it.searchQuery },
    ) { apps, userId, sortType, sortSequence, filterUserApps, filterSystemApps, searchQuery ->
        val filteredApps = apps.filter(userId, filterUserApps, filterSystemApps)
        val sortedApps = when (sortType) {
            SortsType.A2Z -> filteredApps.sortByA2Z(sortSequence)
            SortsType.DATA_SIZE -> filteredApps.sortByDataSize(sortSequence)
            SortsType.INSTALL_TIME -> filteredApps.sortByInstallTime(sortSequence)
            SortsType.UPDATE_TIME -> filteredApps.sortByUpdateTime(sortSequence)
        }
        if (searchQuery.isEmpty()) {
            sortedApps
        } else {
            sortedApps.filter { it.info.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val statistics = apps.map { apps ->
        Statistics(
            selectedCount = apps.count { it.toggleableState != ToggleableState.Off },
            totalCount = apps.size,
            selectedSize = apps.sumOf { it.selectedBytes }
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        initialValue = Statistics(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selectAllState = apps.map { apps ->
        if (apps.isEmpty()) {
            ToggleableState.Off
        } else {
            var hasSelected = false
            var hasUnselected = false
            for (app in apps) {
                when (app.toggleableState) {
                    ToggleableState.On -> hasSelected = true
                    ToggleableState.Off -> hasUnselected = true
                    ToggleableState.Indeterminate -> {
                        hasSelected = true
                        hasUnselected = true
                    }
                }
                if (hasSelected && hasUnselected) break
            }

            when {
                hasSelected && hasUnselected -> ToggleableState.Indeterminate
                hasSelected -> ToggleableState.On
                else -> ToggleableState.Off
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        initialValue = ToggleableState.Off,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun selectApk(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectApk(packageName, userId, selected)
        }
    }

    fun selectInternalData(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectInternalData(packageName, userId, selected)
        }
    }

    fun selectExternalData(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectExternalData(packageName, userId, selected)
        }
    }

    fun selectObbAndMedia(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectObbAndMedia(packageName, userId, selected)
        }
    }

    fun selectAll(packageName: String, userId: Int, toggleableState: ToggleableState) {
        withLock(Dispatchers.IO) {
            val selected = when (toggleableState) {
                ToggleableState.On -> {
                    false
                }

                ToggleableState.Off -> {
                    true
                }

                ToggleableState.Indeterminate -> {
                    true
                }
            }
            DatabaseHelper.appDao.selectAll(packageName, userId, selected)
        }
    }

    fun changeUser(filterUser: Int, userInfo: UserInfo) {
        withLock(Dispatchers.IO) {
            if (filterUser != userInfo.id) {
                App.application.saveInt(KeyFilterBackupUser, userInfo.id)
            }
        }
    }

    inline fun <reified T : Enum<T>> changeSort(selected: Boolean, key: Preferences.Key<String>, value: T) {
        withLock(Dispatchers.IO) {
            if (selected.not()) {
                App.application.saveEnum(key, value)
            }
        }
    }

    fun changeSequence(sequenceBackup: SortsSequence) {
        withLock(Dispatchers.IO) {
            if (sequenceBackup == SortsSequence.ASCENDING) {
                App.application.saveEnum(KeySortsSequenceBackup, SortsSequence.DESCENDING)
            } else {
                App.application.saveEnum(KeySortsSequenceBackup, SortsSequence.ASCENDING)
            }
        }
    }

    fun changeFilter(key: Preferences.Key<Boolean>, value: Boolean) {
        withLock(Dispatchers.IO) {
            App.application.saveBoolean(key, value)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectAllFiltered(userId: Int, selected: Boolean) {
        val packageNames = apps.value.map { it.packageName }
        if (packageNames.isEmpty()) return
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAll(packageNames, userId, selected)
        }
    }

    fun selectAllFiltered(userId: Int, toggleableState: ToggleableState) {
        val selected = when (toggleableState) {
            ToggleableState.On -> false
            ToggleableState.Off -> true
            ToggleableState.Indeterminate -> true
        }
        selectAllFiltered(userId, selected)
    }

    fun reverseSelectionFiltered(userId: Int) {
        val packageNames = apps.value.map { it.packageName }
        if (packageNames.isEmpty()) return
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.reverseSelection(packageNames, userId)
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching {
                val apps = RemoteRootService.getInstalledApps()
                val groupedApps = apps.groupBy { it.userId }
                groupedApps.forEach { (userId, userApps) ->
                    val packageNames = userApps.map { it.packageName }
                    DatabaseHelper.appDao.deleteExcept(packageNames, userId)
                }
                DatabaseHelper.appDao.upsertParcelable(apps)
            }.onFailure {
                LogHelper.e("AppsViewModel", "Failed to refresh apps.", it)
            }
            _isRefreshing.value = false
        }
    }
}
