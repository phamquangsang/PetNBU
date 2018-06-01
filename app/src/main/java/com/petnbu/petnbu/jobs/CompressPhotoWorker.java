package com.petnbu.petnbu.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.petnbu.petnbu.util.Utils;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import androidx.work.Data;
import timber.log.Timber;

public class CompressPhotoWorker extends PhotoWorker {

    @NonNull
    @Override
    public WorkerResult doWork() {
        String photoJson = getInputData().getString(KEY_PHOTO, "");
        Data.Builder outputDataBuilder = new Data.Builder();
        boolean isSuccess = false;

        if(!TextUtils.isEmpty(photoJson)) {
            try {
                List<Photo> photos = new Gson().fromJson(photoJson, new TypeToken<List<Photo>>(){}.getType());

                for (Photo photo : photos) {
                    Uri photoUri = Uri.parse(photo.getOriginUrl());
                    if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                        generateCompressedPhotos(photo);
                    }
                    outputDataBuilder.putString(photoUri.getLastPathSegment(), toJson(photo));
                }
                isSuccess = true;
            } catch (Exception e) {
                e.printStackTrace();
                Timber.i("compress failed %s", e.getMessage());
            }
        }
        outputDataBuilder.putBoolean("result", isSuccess);
        setOutputData(outputDataBuilder.build());
        return WorkerResult.SUCCESS;
    }

    private void generateCompressedPhotos(Photo photo) throws IOException {
        Context context = getApplicationContext();
        Uri fileUri = Uri.parse(photo.getOriginUrl());
        File file = new File(Utils.getPath(context, fileUri));
        String destinationDirectoryPath = context.getFilesDir().getAbsolutePath();

        BitmapFactory.Options bitmapOpts = new BitmapFactory.Options();
        bitmapOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOpts);
        Bitmap bitmap = Bitmap.createBitmap(bitmapOpts.outWidth, bitmapOpts.outHeight, Bitmap.Config.ARGB_8888);
        bitmapOpts.inJustDecodeBounds = false;
        bitmapOpts.inBitmap = bitmap;

        // Origin
        String compressedFileName = String.format("%s", fileUri.getLastPathSegment());
        BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOpts);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(destinationDirectoryPath + File.separator + compressedFileName));
        bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fileOutputStream);
        photo.setOriginUrl(Uri.fromFile(new File(destinationDirectoryPath + File.separator + compressedFileName)).toString());

        // FHD
        int[] resolution = ImageUtils.getResolutionForImage(ImageUtils.FHD, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-FHD", fileUri.getLastPathSegment());
        String savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts);
        photo.setLargeUrl(savedFilePath);

        // HD
        resolution = ImageUtils.getResolutionForImage(ImageUtils.HD, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-HD", fileUri.getLastPathSegment());
        savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts);
        photo.setMediumUrl(savedFilePath);

        // qHD
        resolution = ImageUtils.getResolutionForImage(ImageUtils.qHD, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-qHD", fileUri.getLastPathSegment());
        savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts);
        photo.setSmallUrl(savedFilePath);

        // THUMBNAIL
        resolution = ImageUtils.getResolutionForImage(ImageUtils.THUMBNAIL, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-thumbnail", fileUri.getLastPathSegment());
        savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts);
        photo.setThumbnailUrl(savedFilePath);
    }

    private String createAndSaveResizedBitmap(String path, String destinationDirectoryPath, String compressedFileName,
                                              int width, int height, BitmapFactory.Options opts) throws FileNotFoundException {
        Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(path, opts), width, height, false);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(destinationDirectoryPath + File.separator + compressedFileName));
        bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fileOutputStream);
        bitmap.recycle();
        return Uri.fromFile(new File(destinationDirectoryPath + File.separator + compressedFileName)).toString();
    }
}
