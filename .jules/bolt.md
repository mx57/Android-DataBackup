## 2025-05-15 - [Dynamic Statistics & Search Feature]
**Инсайт:** Placeholder strings for backup statistics and the search bar in `BackupAppsScreen` and `BackupPreviewScreen` were hardcoded and non-functional, which limited the usability and localization of the application.
**Действие:** Implemented a `BackupViewModel` to aggregate statistics (count and size) for selected apps and exposed them via `StateFlow`. Added functional search to `AppsViewModel` by integrating it into the reactive `combine` flow. Ensured all UI components use dynamic string resources with proper placeholders.

## 2025-05-20 - [Real-time Storage Stats on Dashboard]
**Инсайт:** The Dashboard's `StorageCard` used hardcoded placeholders, failing to provide users with actual feedback on storage availability or backup size. Additionally, manually triggered refreshes in ViewModels can lead to coroutine leaks if persistent flow collectors (like `collectLatest`) are launched repeatedly.
**Действие:** Implemented `DashboardViewModel` to compute real storage statistics using `StatFs` and root-level IPC. Ensured that manual refresh logic uses `first()` or similar one-shot collectors to avoid overlapping persistent subscriptions. Updated `IRemoteRootService` to support arbitrary directory size queries.

## 2025-05-25 - [Reactive Multi-User Selection State]
**Инсайт:** The `BackupPreviewScreen` used hardcoded selection states, creating a "dead end" UI. Managing mass selection across multiple users requires grouping package names by `userId` to use optimized DAO `IN (:packageNames)` queries effectively.
**Действие:** Enhanced `SelectableActionButton` to be state-aware. Updated `BackupViewModel` to handle multi-user mass selection by grouping apps before database updates. Synchronized the "Next" button state with the reactive `isAnySelected` property in `Statistics`.

## 2025-06-01 - [Thread Safety & UI Processing]
**Инсайт:** Reactive flows in ViewModels performing O(N) operations (filtering, sorting lists of installed apps) were running on the Main thread by default, leading to potential frame drops during UI updates. Additionally, redundant nested coroutine scopes in Composables hindered efficient task cancellation.
**Действие:** Applied `flowOn(Dispatchers.Default)` to all performance-critical flows in ViewModels. Refactored UI-only state (Select All checkbox) into reactive ViewModel properties. Eliminated redundant `scope.launch` inside `LaunchedEffect` to ensure proper resource management and faster UI response.
