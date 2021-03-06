package com.petnbu.petnbu.jobs

import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.Worker
import com.google.gson.Gson
import com.petnbu.petnbu.api.StorageApi
import com.petnbu.petnbu.extensions.toJson
import com.petnbu.petnbu.model.Photo
import java.io.File
import java.util.concurrent.CountDownLatch

class UploadPhotoWorker : Worker() {

    private val outputDataBuilder = Data.Builder()

    override fun doWork(): Result {
        if(inputData.keyValueMap.containsKey("result")) {
            if(!inputData.getBoolean("result", false)) {
                outputDataBuilder.putBoolean("result", false)
                return Result.SUCCESS
            }
        }

        var isSuccess = false
        val photoName = inputData.getString(KEY_PHOTO, "")

        if (!photoName.isNullOrEmpty()) {
            val jsonPhoto = inputData.getString(photoName!!, "")
            try {
                if (!jsonPhoto.isNullOrEmpty()) {
                    val photo = Gson().fromJson(jsonPhoto, Photo::class.java)
                    val key = photo.originUrl.toUri().lastPathSegment
                    if (!URLUtil.isHttpUrl(photo.originUrl) && !URLUtil.isHttpsUrl(photo.originUrl)) {
                        val countDownLatch = CountDownLatch(1)
                        uploadPhoto(photo, countDownLatch)
                        countDownLatch.await()
                    } else {
                        outputDataBuilder.putString(key, photo.toJson())
                    }
                    isSuccess = true
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
        outputDataBuilder.putBoolean("result", isSuccess)
        outputData = outputDataBuilder.build()
        return Result.SUCCESS
    }

    private fun uploadPhoto(photo: Photo, countDownLatch: CountDownLatch) {
        val urls :List<String> = listOf(photo.originUrl, photo.largeUrl!!, photo.mediumUrl!!, photo.smallUrl!!, photo.thumbnailUrl!!)

        object : StorageApi.OnUploadingImage(urls) {

            override fun onCompleted(result: List<String>) {
                val key = photo.originUrl.toUri().lastPathSegment

                result.onEach {
                    when {
                        it.contains("-FHD") -> photo.largeUrl = it
                        it.contains("-HD") -> photo.mediumUrl = it
                        it.contains("-qHD") -> photo.smallUrl = it
                        it.contains("-thumbnail") -> photo.thumbnailUrl = it
                        else -> photo.originUrl = it
                    }
                }
                urls.onEach {
                    val file = File(it)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                outputDataBuilder.putString(key, photo.toJson())
                countDownLatch.countDown()
            }

            override fun onFailed(e: Exception) {
                countDownLatch.countDown()
            }
        }.start()
    }

    companion object {
        const val KEY_PHOTO = "key-photo"
    }
}
