package com.example.scantesting.permission;

import android.app.Activity;
import android.content.pm.PackageManager;

/**
 * Dynamic permission helper
 * Modified by Administrator on 2020/1/10.
 */

public class PermissionHelper {
    private Activity mActivity;
    private PermissionInterface mPermissionInterface;

    public PermissionHelper(Activity mActivity, PermissionInterface mPermissionInterface) {
        this.mActivity = mActivity;
        this.mPermissionInterface = mPermissionInterface;
    }

    /**
     * Start requesting permissions
     * The method has been judged internally for Android M or above, and no external judgment is required for external use
     * If the device is not M or above and deniedPermissions is null, it will also call back to requestPermissionsSuccess method.
     */
    public void requestPermissions() {
        String[] deniedPermissions = PermissionUtil.getDeniedPermissions(mActivity, mPermissionInterface.getPermissions());
        if (deniedPermissions != null && deniedPermissions.length > 0) {
            PermissionUtil.requestPermissions(mActivity, deniedPermissions, mPermissionInterface.getPermissionsRequestCode());
        } else {
            mPermissionInterface.requestPermissionsSuccess();
        }
    }

    public boolean requestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == mPermissionInterface.getPermissionsRequestCode()) {
            boolean isAllGranted = true;//Whether all permissions are authorized
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                //Fully authorized
                mPermissionInterface.requestPermissionsSuccess();
            } else {
                //Missing permissions
                mPermissionInterface.requestPermissionFail();
            }
            return true;
        }
        return false;
    }
}
