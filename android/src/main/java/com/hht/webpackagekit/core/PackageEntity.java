package com.hht.webpackagekit.core;

import java.util.List;

/**
 * 离线包Index信息
 */
public class PackageEntity {
    private List<PackageInfo> items;

    public void setItems(List<PackageInfo> items) {
        this.items = items;
    }

    public List<PackageInfo> getItems() {
        return items;
    }
}
