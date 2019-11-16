package com.hht.webpackagekit.core;

import android.text.TextUtils;
import android.util.Log;

import com.hht.webpackagekit.core.util.MD5Utils;

/**
 * 离线包信息
 */
public class PackageInfo {
    //离线包ID
    private String packageId;

    //离线包版本号
    private String version = "1";

    //离线包的状态 {@link PackageStatus}
    private int status = PackageStatus.onLine;

    //是否是patch包
    private boolean isPatch;

    //离线包md值 由后端下发
    private String md5;


    public String getPackageId() {
        return packageId;
    }

    //获取离线包下载地址
    public String getDownloadUrl() {
        // 计算下载文件名称，即 packageId + preVersion + " -> " + version 对应的 md5 值，例如 'mwbp1 -> 2'
        int versionInt = Integer.parseInt(version);
        int preVersionInt = versionInt - 1;
        String fileName = MD5Utils.getMD5(packageId + preVersionInt + " -> " + versionInt);
        Log.d("downloadUrl", fileName);
        return Constants.BASE_DOWNLOAD_URL + "/" + packageId + "/" + fileName;
    }

    public String getVersion() {
        return version;
    }

    public int getStatus() {
        return status;
    }

    public boolean isPatch() {
        return isPatch;
    }

    public String getMd5() {
        return md5;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageInfo)) {
            return false;
        }
        PackageInfo that = (PackageInfo) obj;
        return TextUtils.equals(packageId, that.packageId);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + packageId == null ? 0 : packageId.hashCode();
        return result;
    }
}
