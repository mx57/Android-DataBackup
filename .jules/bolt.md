## 2025-05-15 - [Dynamic Statistics & Search Feature]
**Инсайт:** Placeholder strings for backup statistics and the search bar in `BackupAppsScreen` and `BackupPreviewScreen` were hardcoded and non-functional, which limited the usability and localization of the application.
**Действие:** Implemented a `BackupViewModel` to aggregate statistics (count and size) for selected apps and exposed them via `StateFlow`. Added functional search to `AppsViewModel` by integrating it into the reactive `combine` flow. Ensured all UI components use dynamic string resources with proper placeholders.

## 2025-05-20 - [Real-time Storage Stats on Dashboard]
**Инсайт:** The Dashboard's `StorageCard` used hardcoded placeholders, failing to provide users with actual feedback on storage availability or backup size. Additionally, manually triggered refreshes in ViewModels can lead to coroutine leaks if persistent flow collectors (like `collectLatest`) are launched repeatedly.
**Действие:** Implemented `DashboardViewModel` to compute real storage statistics using `StatFs` and root-level IPC. Ensured that manual refresh logic uses `first()` or similar one-shot collectors to avoid overlapping persistent subscriptions. Updated `IRemoteRootService` to support arbitrary directory size queries.

## 2025-05-25 - [Reactive Multi-User Selection State]
**Инсайт:** The `BackupPreviewScreen` used hardcoded selection states, creating a "dead end" UI. Managing mass selection across multiple users requires grouping package names by `userId` to use optimized DAO `IN (:packageNames)` queries effectively.
**Действие:** Enhanced `SelectableActionButton` to be state-aware. Updated `BackupViewModel` to handle multi-user mass selection by grouping apps before database updates. Synchronized the "Next" button state with the reactive `isAnySelected` property in `Statistics`.

## 2026-07-01 - [Thread Safety & UI Processing]
**Инсайт:** Reactive flows in ViewModels performing O(N) operations (filtering, sorting lists of installed apps) were running on the Main thread by default, leading to potential frame drops during UI updates. Additionally, redundant nested coroutine scopes in Composables hindered efficient task cancellation.
**Действие:** Applied `flowOn(Dispatchers.Default)` to all performance-critical flows in ViewModels. Refactored UI-only state (Select All checkbox) into reactive ViewModel properties. Eliminated redundant `scope.launch` inside `LaunchedEffect` to ensure proper resource management and faster UI response.

## 2026-07-02 - [Consolidated App Sync & IPC Reliability]
**Инсайт:** Sequential IPC calls to fetch app metadata and storage stats resulted in redundant package iterations and increased binder overhead. Additionally, the root service binding logic could hang if called when the service was already connected.
**Действие:** Implemented a consolidated 'getInstalledApps' AIDL method and updated 'RemoteRootService' to fetch all data in a single pass. Fixed the suspension hang in 'bindService' by ensuring 'continuation.resume' is called for already connected services. Added directory existence checks before calling native size calculation to improve robustness.

## 2026-07-03 - [Pull-to-Refresh & Empty States]
**Инсайт:** Implementing `PullToRefreshBox` in Compose requires specific imports (`androidx.compose.material3.pulltorefresh.PullToRefreshBox`) and the `@ExperimentalMaterial3Api` annotation. Furthermore, the pull-to-refresh gesture won't trigger if the content (like an empty list placeholder) is not explicitly scrollable.
**Действие:** Added `verticalScroll` with a `rememberScrollState()` to the empty state Column in `BackupAppsScreen` to ensure it captures swipe gestures for refreshing. Verified that `PullToRefreshBox` correctly encapsulates the `AnimatedContent` for smooth transitions between empty and populated states.

## 2026-07-04 - [Resource Safety & DB Synchronization]
**Инсайт:** In IPC operations involving 'ParcelFileDescriptor', failing to explicitly close the descriptor and its associated streams can lead to file descriptor leaks. Additionally, a simple "upsert" strategy for app list synchronization leaves records for uninstalled apps in the database.
**Действие:** Wrapped 'ParcelFileDescriptor' and 'AutoCloseInputStream' in '.use' blocks in 'RemoteRootService.kt'. Implemented 'deleteExcept' in 'AppDao' and updated 'AppsViewModel' and 'AppsUpdateWorker' to prune the database of uninstalled apps by package name and user ID during synchronization. Refined search bar UX to prioritize clearing text over closing the bar.

## 2026-07-06 - [Dashboard UX & Localization Consistency]
**Инсайт:** Placeholder actions on the Dashboard that remain silent upon interaction lead to poor user feedback. Additionally, hardcoded labels in reusable UI components like 'StorageCard' break localization support and consistency.
**Действие:** Implemented a Snackbar-based 'showComingSoon()' feedback mechanism for all placeholder dashboard actions. Refactored 'StorageCard' to accept dynamic string parameters for storage legend labels, ensuring full localization. Integrated Material 3 'PullToRefreshBox' on the Dashboard, backed by a new 'isRefreshing' state in 'DashboardViewModel'.

## 2026-07-07 - [Filtered Mass Selection Logic]
**Инсайт:** When implementing mass selection (Select All, Unselect All, Reverse Selection) in a screen with filters and search, performing selection operations on the entire list of apps disregards the user's filtered view. Operating directly on the filtered list of package names retrieved from the UI's reactive StateFlow and performing atomic SQLite batch updates (`UPDATE ... WHERE packageName IN (:packageNames)`) prevents inconsistent UI states and O(N) iteration overhead.
**Действие:** Implemented an optimized `reverseSelection` query in `AppDao` that inverts all selection flags concurrently. Added `selectAllFiltered` and `reverseSelectionFiltered` in `AppsViewModel` to update the DB using only package names currently in the filtered list. Integrated the "Actions" section containing Select All, Unselect All, and Reverse Selection buttons in the bottom sheet using non-persistent action styling (`selected = false`).

## 2026-07-19 - [Robust Database Sync & Search UX]
**Инсайт:** SQLite and Room treat `NOT IN (:emptyCollection)` unexpectedly because SQL compiles empty collections into `NOT IN (NULL)`, which evaluates to UNKNOWN and silently deletes absolutely nothing. Additionally, standard Android UX expects search screens to dismiss search fields first before collapsing/popping the backstack.
**Действие:** Added `deleteByUserId` query to `AppDao.kt`. Configured `AppsViewModel` and `AppsUpdateWorker` to fallback to `deleteByUserId(userId)` when clearing all applications for active or inactive users. Refined 'BackupAppsScreen' search experience using Compose 'BackHandler' to collapse search fields and clear queries on system back events. Cleaned up obsolete localized keys ('installation_time', 'sort') causing merging warnings.
