package com.petnbu.petnbu.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.petnbu.petnbu.util.Utils;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.work.Data;
import timber.log.Timber;

public class CompressPhotoWorker extends PhotoWorker {

    @NonNull
    @Override
    public WorkerResult doWork() {
        WorkerResult workerResult = WorkerResult.FAILURE;

        String photoJson = getInputData().getString(KEY_PHOTO, "");
        if(!TextUtils.isEmpty(photoJson)) {
            try {
                List<Photo> photos = new Gson().fromJson(photoJson, new TypeToken<List<Photo>>(){}.getType());
                Data.Builder outputDataBuilder = new Data.Builder();

                for (Photo photo : photos) {
                    Uri photoUri = Uri.parse(photo.getOriginUrl());
                    if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                        generateCompressedPhotos(photo);
                    }
                    outputDataBuilder.putString(photoUri.getLastPathSegment(), toJson(photo));
                }
                setOutputData(outputDataBuilder.build());

                workerResult = WorkerResult.SUCCESS;
            } catch (IOException e) {
                e.printStackTrace();
                Timber.i("compress failed %s", e.getMessage());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                Timber.i("compress failed %s", e.getMessage());
            }
        }
        return workerResult;
    }

    private void generateCompressedPhotos(Photo photo) throws IOException {
        Context context = getApplicationContext();
        Uri fileUri = Uri.parse(photo.getOriginUrl());
        File file = new File(Utils.getPath(context, fileUri));
        String destinationDirectoryPath = context.getFilesDir().getAbsolutePath();

        // Origin
        String compressedFileName = String.format("%s", fileUri.getLastPathSegment());
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        FileOutputStream fileOutputStream = new FileOutputStream(new File(destinationDirectoryPath + File.separator + compressedFileName));
        bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fileOutputStream);
        photo.setOriginUrl(Uri.fromFile(new File(destinationDirectoryPath + File.separator + compressedFileName)).toString());
        bitmap.recycle();

        // FHD
        int[] resolution = ImageUtils.getResolutionForImage(ImageUtils.FHD, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-FHD", fileUri.getLastPathSegment());
        String savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1]);
        photo.setLargeUrl(savedFilePath);

        // HD
        resolution = ImageUtils.getResolutionForImage(ImageUtils.HD, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-HD", fileUri.getLastPathSegment());
        savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1]);
        photo.setMediumUrl(savedFilePath);

        // qHD
        resolution = ImageUtils.getResolutionForImage(ImageUtils.qHD, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-qHD", fileUri.getLastPathSegment());
        savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1]);
        photo.setSmallUrl(savedFilePath);

        // THUMBNAIL
        resolution = ImageUtils.getResolutionForImage(ImageUtils.THUMBNAIL, photo.getWidth(), photo.getHeight());
        compressedFileName = String.format("%s-thumbnail", fileUri.getLastPathSegment());
        savedFilePath = createAndSaveResizedBitmap(file.getAbsolutePath(), destinationDirectoryPath, compressedFileName, resolution[0], resolution[1]);
        photo.setThumbnailUrl(savedFilePath);
    }

    private String createAndSaveResizedBitmap(String path, String destinationDirectoryPath, String compressedFileName, int width, int height) throws FileNotFoundException {
        Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(path), width, height, false);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(destinationDirectoryPath + File.separator + compressedFileName));
        bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fileOutputStream);
        bitmap.recycle();
        return Uri.fromFile(new File(destinationDirectoryPath + File.separator + compressedFileName)).toString();
    }
}
