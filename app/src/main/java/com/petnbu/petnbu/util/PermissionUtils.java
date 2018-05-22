package com.petnbu.petnbu.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

public final class PermissionUtils {

    public static final boolean requestPermissions(Activity activity, int requestCode, String... permissions) {
        ArrayList<String> notGrantedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermissions.add(permission);
            }
        }
        if(!notGrantedPermissions.isEmpty()) {
            String[] notGrantedPermissionsArray = new String[]{};
            ActivityCompat.requestPermissions(activity, notGrantedPermissions.toArray(notGrantedPermissionsArray), requestCode);
            return true;
        }
        return false;
    }

    public static final boolean requestPermissions(Fragment fragment, int requestCode, String... permissions) {
        ArrayList<String> notGrantedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(fragment.getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermissions.add(permission);
            }
        }
        if(!notGrantedPermissions.isEmpty()) {
            String[] notGrantedPermissionsArray = new String[]{};
            fragment.requestPermissions(notGrantedPermissions.toArray(notGrantedPermissionsArray), requestCode);
            return true;
        }
        return false;
    }

    public static final void requestPersistablePermission(Activity activity, Intent data, Uri uri) {
        if (Build.VERSION.SDK_INT >= 19) {
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            try {
                if (uri != null) {
                    activity.getContentResolver()
                            .takePersistableUriPermission(uri, takeFlags);
                } else {
                    //todo notify user something wrong with selected photo
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}
