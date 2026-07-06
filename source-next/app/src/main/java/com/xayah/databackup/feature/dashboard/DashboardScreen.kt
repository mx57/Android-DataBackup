package com.xayah.databackup.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.App
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.feature.Backup
import com.xayah.databackup.ui.component.ActionButton
import com.xayah.databackup.ui.component.SmallActionButton
import com.xayah.databackup.ui.component.StorageCard
import com.xayah.databackup.util.BackupDir
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.readString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, viewModel: DashboardViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val backupDir by App.application.readString(BackupDir).collectAsStateWithLifecycle(initialValue = BackupDir.second)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoonMessage = stringResource(R.string.feature_coming_soon)

    fun showComingSoon() {
        scope.launch {
            snackbarHostState.showSnackbar(comingSoonMessage)
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showComingSoon() }) {
                        BadgedBox(
                            badge = {
                                Badge()
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info),
                                contentDescription = stringResource(R.string.info)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showComingSoon() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshStorageStats() }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val total = storageStats.totalBytes.toFloat()
                    val freeRatio = if (total > 0) storageStats.freeBytes / total else 0f
                    val backupsRatio = if (total > 0) storageStats.backupsBytes / total else 0f
                    val otherRatio = if (total > 0) storageStats.otherBytes / total else 0f
                    val usedPercent = if (total > 0) ((total - storageStats.freeBytes) / total * 100).toInt() else 0

                    StorageCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        free = freeRatio,
                        other = otherRatio,
                        backups = backupsRatio,
                        title = stringResource(R.string.internal_storage),
                        subtitle = backupDir,
                        progress = "$usedPercent%",
                        storage = (storageStats.totalBytes - storageStats.freeBytes).formatToStorageSize,
                        backupsLabel = stringResource(R.string.storage_backups),
                        otherLabel = stringResource(R.string.storage_other),
                        freeLabel = stringResource(R.string.storage_free),
                    ) {
                        viewModel.refreshStorageStats()
                    }

                    Text(
                        text = stringResource(R.string.dashboard_actions),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SmallActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentSize(),
                            icon = ImageVector.vectorResource(R.drawable.ic_archive),
                            title = stringResource(R.string.backup),
                            subtitle = stringResource(R.string.dashboard_backup_desc)
                        ) {
                            navController.navigateSafely(Backup)
                        }

                        SmallActionButton(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentSize(),
                            icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
                            title = stringResource(R.string.restore),
                            subtitle = stringResource(R.string.dashboard_restore_desc)
                        ) {
                            showComingSoon()
                        }
                    }

                    ActionButton(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .wrapContentSize(),
                        icon = ImageVector.vectorResource(R.drawable.ic_clock),
                        title = stringResource(R.string.history),
                        subtitle = stringResource(R.string.dashboard_history_desc)
                    ) {
                        showComingSoon()
                    }

                    ActionButton(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .wrapContentSize(),
                        icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
                        title = stringResource(R.string.cloud),
                        subtitle = stringResource(R.string.dashboard_cloud_desc)
                    ) {
                        showComingSoon()
                    }

                    ActionButton(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .wrapContentSize(),
                        icon = ImageVector.vectorResource(R.drawable.ic_calendar_check),
                        title = stringResource(R.string.schedule),
                        subtitle = stringResource(R.string.dashboard_schedule_desc)
                    ) {
                        showComingSoon()
                    }
                }

                Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
            }
        }
    }
}
