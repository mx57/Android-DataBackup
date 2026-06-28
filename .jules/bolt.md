## 2025-05-15 - [Dynamic Statistics & Search Feature]
**Инсайт:** Placeholder strings for backup statistics and the search bar in `BackupAppsScreen` and `BackupPreviewScreen` were hardcoded and non-functional, which limited the usability and localization of the application.
**Действие:** Implemented a `BackupViewModel` to aggregate statistics (count and size) for selected apps and exposed them via `StateFlow`. Added functional search to `AppsViewModel` by integrating it into the reactive `combine` flow. Ensured all UI components use dynamic string resources with proper placeholders.

## 2025-05-20 - [Real-time Storage Stats on Dashboard]
**Инсайт:** The Dashboard's `StorageCard` used hardcoded placeholders, failing to provide users with actual feedback on storage availability or backup size. Additionally, manually triggered refreshes in ViewModels can lead to coroutine leaks if persistent flow collectors (like `collectLatest`) are launched repeatedly.
**Действие:** Implemented `DashboardViewModel` to compute real storage statistics using `StatFs` and root-level IPC. Ensured that manual refresh logic uses `first()` or similar one-shot collectors to avoid overlapping persistent subscriptions. Updated `IRemoteRootService` to support arbitrary directory size queries.
