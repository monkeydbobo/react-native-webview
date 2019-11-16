package com.hht.webpackagekit.inner;

import android.content.Context;
import android.text.TextUtils;

import com.hht.lib.bsdiff.PatchUtils;
import com.hht.webpackagekit.core.PackageEntity;
import com.hht.webpackagekit.core.PackageInfo;
import com.hht.webpackagekit.core.PackageInstaller;
import com.hht.webpackagekit.core.util.FileUtils;
import com.hht.webpackagekit.core.util.GsonUtils;
import com.hht.webpackagekit.core.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * 单个离线包安装器
 */
public class PackageInstallerImpl implements PackageInstaller {
    private Context context;

    public PackageInstallerImpl(Context context) {
        this.context = context;
    }

    /**
     * 下载的离线包download.zip或预置在assets的离线包package.zip
     * 如果是patch文件 merge.zip
     * 更新后的zip文件 update.zip
     */
    @Override
    public boolean install(PackageInfo packageInfo, boolean isAssets) {
        //获取刚下载的离线包download.zip的路径，或者是预先加载到assets的离线包package.zip的路径
        String downloadFile =
            isAssets ? FileUtils.getPackageAssetsName(context, packageInfo.getPackageId(), packageInfo.getVersion())
                : FileUtils.getPackageDownloadName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        String willCopyFile = downloadFile;

        //获取即将被更新的离线包update.zip的路径
        String updateFile =
            FileUtils.getPackageUpdateName(context, packageInfo.getPackageId(), packageInfo.getVersion());

        boolean isSuccess = true;
        String lastVersion = getLastVersion(packageInfo.getPackageId());
        if (packageInfo.isPatch() && TextUtils.isEmpty(lastVersion)) {
            Logger.e("资源为patch ,但是上个版本信息没有数据，无法patch!");
            return false;
        }

        //如果是增量包，则合并增量包
        if (packageInfo.isPatch()) {
            //获取将被更新的离线包update.zip
            String baseFile = FileUtils.getPackageUpdateName(context, packageInfo.getPackageId(), lastVersion);
            //获取即将合并成的包
            String mergePatch =
                FileUtils.getPackageMergePatch(context, packageInfo.getPackageId(), packageInfo.getVersion());
            //合并 已经即将被更新的包download.zip或res.zip 和 本地即将被更新的离线包update.zip，生成merge.zip
            //并删除刚下载的离线包download.zip或res.zip
            int status = -1;
            try {
                status = PatchUtils.getInstance().patch(baseFile, mergePatch, downloadFile);
            } catch (Exception ignore) {
                Logger.e("patch error " + ignore.getMessage());
            }
            if (status == 0) {
                willCopyFile = mergePatch;
                FileUtils.deleteFile(downloadFile);
            } else {
                isSuccess = false;
            }
        }
        if (!isSuccess) {
            Logger.e("资源patch merge 失败！");
            return false;
        }

        //拷贝downloadFile(download.zip 或 res.zip)或合并增量包生成的merge.zip到update.zip,并删除刚被拷贝的文件
        isSuccess = FileUtils.copyFileCover(willCopyFile, updateFile);
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "copy file error ");
            return false;
        }
        isSuccess = FileUtils.delFile(willCopyFile);
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "delete will copy file error ");
            return false;
        }

        //解压已经更新过的update.zip资源包到work目录下
        String workPath = FileUtils.getPackageWorkName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        try {
            isSuccess = FileUtils.unZipFolder(updateFile, workPath);
        } catch (Exception e) {
            isSuccess = false;
        }
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "unZipFolder error ");
            return false;
        }
        if (isSuccess) {
            FileUtils.deleteFile(willCopyFile);
            cleanOldFileIfNeed(packageInfo.getPackageId(), packageInfo.getVersion(), lastVersion);
        }
        return isSuccess;
    }

    private void cleanOldFileIfNeed(String packageId, String version, String lastVersion) {
        String path = FileUtils.getPackageRootByPackageId(context, packageId);
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        File[] versionList = file.listFiles();
        if (versionList == null || versionList.length == 0) {
            return;
        }
        List<File> deleteFiles = new ArrayList<>();
        for (File item : versionList) {
            if (TextUtils.equals(version, item.getName()) || TextUtils.equals(lastVersion, item.getName())) {
                continue;
            }
            deleteFiles.add(item);
        }
        for (File file1 : deleteFiles) {
            FileUtils.deleteDir(file1);
        }
    }

    //根据packageId获取对应包的版本
    private String getLastVersion(String packageId) {
        //获取packageIndex.json文件（items数组中包含每个包的信息），并基于该文件生成PackageEntity实例localPackageEntity
        String packageIndexFile = FileUtils.getPackageIndexFileName(context);
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(packageIndexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            return "";
        }
        PackageEntity localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        if (localPackageEntity == null || localPackageEntity.getItems() == null) {
            return "";
        }

        //从localPackageEntity实例中获取items，PackageInfo的数组集合，一个PackageInfo对应一个离线资源包，根据packageId查找对应包的最新版本
        List<PackageInfo> list = localPackageEntity.getItems();
        PackageInfo info = new PackageInfo();
        info.setPackageId(packageId);
        int index = list.indexOf(info);
        if (index >= 0) {
            return list.get(index).getVersion();
        }
        return "";
    }
}
