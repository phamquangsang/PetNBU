package com.petnbu.petnbu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_READ_EXTERNAL_PERMISSIONS = 8;

    public static final int GALLERY_KITKAT_INTENT_CALLED = 2;
    public static final int GALLERY_INTENT_CALLED = 3;

    protected boolean requestReadExternalPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_READ_EXTERNAL_PERMISSIONS);
            return true;
        }
        return false;
    }

    protected void openPhotoGallery(boolean allowMultiPick) {
        if (Build.VERSION.SDK_INT < 19) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, GALLERY_INTENT_CALLED);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiPick);
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_KITKAT_INTENT_CALLED);
        }
    }

    protected void requestPersistablePermission(Intent data, Uri uri) {
        if (Build.VERSION.SDK_INT >= 19) {
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            try {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } else {
                    //todo notify user something wrong with selected photo
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}
