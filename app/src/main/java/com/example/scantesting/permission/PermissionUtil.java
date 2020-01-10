package com.example.scantesting.permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

/**
 * Dynamic permission request tool class
 * Modified by Administrator on 2020/1/10.
 */

public class PermissionUtil {
    /**
     * Determine if there is a permission
     *
     * @param context
     * @param permission
     * @return
     */
    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pop-up dialog box request permission
     *
     * @param activity
     * @param permissions
     * @param reqestCode
     */
    public static void requestPermissions(Activity activity, String[] permissions, int reqestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permissions, reqestCode);
        }
    }

    /**
     * Returns missing permissions
     *
     * @param context
     * @param permissions
     * @return null it means no missing permissions
     */
    public static String[] getDeniedPermissions(Context context, String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> deniedPermissionList = new ArrayList<>();
            for (String permisson : permissions) {
                if (context.checkSelfPermission(permisson) != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissionList.add(permisson);
                }
            }
            int size = deniedPermissionList.size();
            if (size > 0) {
                return deniedPermissionList.toArray(new String[deniedPermissionList.size()]);
            }
        }
        return null;
    }
}
