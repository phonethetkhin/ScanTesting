package com.example.scantesting.permission;

/**
 * Permission Request Interface
 * Modified by Administrator on 2020/1/10.
 */

public interface PermissionInterface {
    /**
     * Set request code
     * @return
     */
    int getPermissionsRequestCode();

    /**
     * Set required permissions
     * @return
     */
    String[] getPermissions();

    /**
     * Permission request succeeded
     */
    void requestPermissionsSuccess();

    /**
     * Permission request failed
     */
    void requestPermissionFail();
}
