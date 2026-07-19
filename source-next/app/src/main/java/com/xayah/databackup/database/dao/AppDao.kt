package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.AppInfo
import com.xayah.databackup.database.entity.AppParcelable
import com.xayah.databackup.database.entity.AppStorage
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Upsert(entity = App::class)
    suspend fun upsert(apps: List<App>)

    @Upsert(entity = App::class)
    suspend fun upsertInfo(apps: List<AppInfo>)

    @Upsert(entity = App::class)
    suspend fun upsertStorage(apps: List<AppStorage>)

    @Upsert(entity = App::class)
    suspend fun upsertParcelable(apps: List<AppParcelable>)

    @Query("SELECT * from apps")
    fun loadFlowApps(): Flow<List<App>>

    @Query("UPDATE apps SET option_apk = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectApk(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_internalData = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectInternalData(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_externalData = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectExternalData(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_obbAndMedia = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectObbAndMedia(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_apk = :selected, option_internalData = :selected, option_externalData = :selected, option_obbAndMedia = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectAll(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_apk = :selected, option_internalData = :selected, option_externalData = :selected, option_obbAndMedia = :selected WHERE packageName IN (:packageNames) AND userId = :userId")
    suspend fun selectAll(packageNames: List<String>, userId: Int, selected: Boolean)

    /**
     * Inverts the selection state of all backup options for the specified apps.
     */
    @Query("UPDATE apps SET option_apk = NOT option_apk, option_internalData = NOT option_internalData, option_externalData = NOT option_externalData, option_obbAndMedia = NOT option_obbAndMedia WHERE packageName IN (:packageNames) AND userId = :userId")
    suspend fun reverseSelection(packageNames: List<String>, userId: Int)

    @Query("DELETE FROM apps WHERE packageName = :packageName AND userId = :userId")
    suspend fun delete(packageName: String, userId: Int)

    @Query("DELETE FROM apps WHERE packageName NOT IN (:packageNames) AND userId = :userId")
    suspend fun deleteExcept(packageNames: List<String>, userId: Int)

    @Query("DELETE FROM apps WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int)

    @Query("SELECT DISTINCT userId FROM apps")
    suspend fun getDistinctUserIds(): List<Int>
}
