package com.petnbu.petnbu.jobs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.Worker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.petnbu.petnbu.extensions.toJson
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.ImageUtils
import com.petnbu.petnbu.util.Utils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class CompressPhotoWorker : Worker() {

    override fun doWork(): WorkerResult {
        val photoJson = inputData.getString(KEY_PHOTO, "")
        val outputDataBuilder = Data.Builder()
        var isSuccess = false

        if (!photoJson.isNullOrEmpty()) {
            try {
                val photos = Gson().fromJson<List<Photo>>(photoJson, object : TypeToken<List<Photo>>() {}.type)

                for (photo in photos) {
                    val photoUri = photo.originUrl.toUri()
                    if (!URLUtil.isHttpUrl(photo.originUrl) && !URLUtil.isHttpsUrl(photo.originUrl)) {
                        generateCompressedPhotos(photo)
                    }
                    outputDataBuilder.putString(photoUri.lastPathSegment, photo.toJson())
                }
                isSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.i("compress failed %s", e.message)
            }
        }

        outputDataBuilder.putBoolean("result", isSuccess)
        outputData = outputDataBuilder.build()
        return WorkerResult.SUCCESS
    }

    @Throws(IOException::class)
    private fun generateCompressedPhotos(photo: Photo) {
        val context = applicationContext
        val fileUri = photo.originUrl.toUri()
        val file = File(Utils.getPath(context, fileUri)!!)
        val destinationDirectoryPath = context.filesDir.absolutePath

        val bitmapOpts = BitmapFactory.Options()
        bitmapOpts.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, bitmapOpts)
        val bitmap = Bitmap.createBitmap(bitmapOpts.outWidth, bitmapOpts.outHeight, Bitmap.Config.ARGB_8888)
        bitmapOpts.inJustDecodeBounds = false
        bitmapOpts.inBitmap = bitmap

        // Origin
        var compressedFileName = String.format("%s", fileUri.lastPathSegment)
        BitmapFactory.decodeFile(file.absolutePath, bitmapOpts)
        val fileOutputStream = FileOutputStream(File(destinationDirectoryPath + File.separator + compressedFileName))
        bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fileOutputStream)
        photo.originUrl = Uri.fromFile(File(destinationDirectoryPath + File.separator + compressedFileName)).toString()

        // FHD
        var resolution = ImageUtils.getResolutionForImage(ImageUtils.FHD, photo.width, photo.height)
        compressedFileName = String.format("%s-FHD", fileUri.lastPathSegment)
        var savedFilePath = createAndSaveResizedBitmap(file.absolutePath, destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts)
        photo.largeUrl = savedFilePath

        // HD
        resolution = ImageUtils.getResolutionForImage(ImageUtils.HD, photo.width, photo.height)
        compressedFileName = String.format("%s-HD", fileUri.lastPathSegment)
        savedFilePath = createAndSaveResizedBitmap(file.absolutePath, destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts)
        photo.mediumUrl = savedFilePath

        // qHD
        resolution = ImageUtils.getResolutionForImage(ImageUtils.qHD, photo.width, photo.height)
        compressedFileName = String.format("%s-qHD", fileUri.lastPathSegment)
        savedFilePath = createAndSaveResizedBitmap(file.absolutePath, destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts)
        photo.smallUrl = savedFilePath

        // THUMBNAIL
        resolution = ImageUtils.getResolutionForImage(ImageUtils.THUMBNAIL, photo.width, photo.height)
        compressedFileName = String.format("%s-thumbnail", fileUri.lastPathSegment)
        savedFilePath = createAndSaveResizedBitmap(file.absolutePath, destinationDirectoryPath, compressedFileName, resolution[0], resolution[1], bitmapOpts)
        photo.thumbnailUrl = savedFilePath
    }

    @Throws(FileNotFoundException::class)
    private fun createAndSaveResizedBitmap(path: String, destinationDirectoryPath: String, compressedFileName: String,
                                           width: Int, height: Int, opts: BitmapFactory.Options): String {
        val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(path, opts), width, height, false)
        val fileOutputStream = FileOutputStream(File(destinationDirectoryPath + File.separator + compressedFileName))
        fileOutputStream.use {
            bitmap.compress(Bitmap.CompressFormat.WEBP, 75, it)
        }
        bitmap.recycle()
        return Uri.fromFile(File(destinationDirectoryPath + File.separator + compressedFileName)).toString()
    }


    companion object {
        const val KEY_PHOTO = "key-photo"

        @JvmStatic
        fun data(photo: Photo): Data = data(ArrayList<Photo>(1).apply { add(photo) })

        @JvmStatic
        fun data(photos: List<Photo>): Data = Data.Builder()
                .putString(KEY_PHOTO, photos.toJson())
                .build()
    }
}
