## 2025-05-15 - [Dynamic Statistics & Search Feature]
**Инсайт:** Placeholder strings for backup statistics and the search bar in `BackupAppsScreen` and `BackupPreviewScreen` were hardcoded and non-functional, which limited the usability and localization of the application.
**Действие:** Implemented a `BackupViewModel` to aggregate statistics (count and size) for selected apps and exposed them via `StateFlow`. Added functional search to `AppsViewModel` by integrating it into the reactive `combine` flow. Ensured all UI components use dynamic string resources with proper placeholders.
