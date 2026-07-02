package com.xayah.databackup.rootservice;

interface IRemoteRootService {
    ParcelFileDescriptor getInstalledAppInfos();
    ParcelFileDescriptor getInstalledAppStorages();
    ParcelFileDescriptor getInstalledApps();
    List<UserInfo> getUsers();
    long getDirSize(String path);
}
