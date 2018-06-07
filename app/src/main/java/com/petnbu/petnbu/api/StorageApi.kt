package com.petnbu.petnbu.api


import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.petnbu.petnbu.BuildConfig
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Quang Quang on 11/23/2016.
 */

object StorageApi {

    val storageRef: StorageReference
        get() {
            val storage = FirebaseStorage.getInstance().getReferenceFromUrl(BuildConfig.FIREBASE_STORAGE_URL)
            return storage.child("pets").child("photos")
        }

    // map files to file

    fun updateImage(uri: Uri, fileName: String?, onResultListener: OnResultListener) {
        Timber.i("updateImage: update file %s", uri.toString())
        val ref = storageRef.child("image_" + System.currentTimeMillis() + fileName)
        ref.putFile(uri)
                .addOnSuccessListener { taskSnapshot -> onResultListener.onSuccess(fileName, taskSnapshot) }
                .addOnFailureListener { e -> onResultListener.onFailure(fileName, e) }
    }

    fun updateBitmap(bitmap: Bitmap, fileName: String, onResultListener: OnResultListener) {
        Timber.i("updateBitmap $fileName")
        val ref = storageRef.child("image_" + System.currentTimeMillis() + fileName)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val data = outputStream.toByteArray()
        ref.putBytes(data)
                .addOnSuccessListener { taskSnapshot -> onResultListener.onSuccess(fileName, taskSnapshot) }
                .addOnFailureListener { e -> onResultListener.onFailure(fileName, e) }
    }

    abstract class OnUploadingImage(private val mList: List<String>,
                                    private val mTotalImage: AtomicInteger = AtomicInteger(mList.size),
                                    private val mResult: MutableList<String> = ArrayList(mList.size)) {

        private val mOnResultListener = object : OnResultListener {
            override fun onSuccess(fileName: String?, taskSnapshot: UploadTask.TaskSnapshot) {
                mTotalImage.decrementAndGet()
                mResult.add(taskSnapshot.downloadUrl!!.toString())
                taskSnapshot.downloadUrl?.let { mResult.add(it.toString()) }
                if (mTotalImage.get() == 0) {
                    onCompleted(mResult)
                }
            }

            override fun onFailure(fileName: String?, e: Exception) {
                mTotalImage.decrementAndGet()
                onFailed(e)
            }
        }


        fun start() {
            for (filePath in mList) {
                val fileUri = filePath.toUri()
                updateImage(fileUri, fileUri.lastPathSegment, mOnResultListener)
            }
        }

        abstract fun onCompleted(result: List<String>)

        abstract fun onFailed(e: Exception)
    }

    interface OnResultListener {

        fun onSuccess(fileName: String?, taskSnapshot: UploadTask.TaskSnapshot)

        fun onFailure(fileName: String?, e: Exception)
    }
}
