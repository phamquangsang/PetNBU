package com.petnbu.petnbu.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class Photo(var originUrl: String, var largeUrl: String? = null, var mediumUrl: String? = null,
            var smallUrl: String? = null, var thumbnailUrl: String? = null, var width: Int = 0, var height: Int = 0) : Parcelable {

    //origin
    //largeUrl for      FHD         1920x1080
    //mediumUrl for     HD          1280x720
    //smallUrl for      qHD         960x540
    //thumbnailUrl for  thumbnail   150x150

    constructor() : this("")

    constructor(parcel: Parcel) : this(parcel.readString()) {
        originUrl = parcel.readString()
        largeUrl = parcel.readString()
        mediumUrl = parcel.readString()
        smallUrl = parcel.readString()
        thumbnailUrl = parcel.readString()
        width = parcel.readInt()
        height = parcel.readInt()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val photo = other as Photo?

        if (width != photo!!.width) return false
        if (height != photo.height) return false
        if (if (originUrl != null) originUrl != photo.originUrl else photo.originUrl != null)
            return false
        if (if (largeUrl != null) largeUrl != photo.largeUrl else photo.largeUrl != null)
            return false
        if (if (mediumUrl != null) mediumUrl != photo.mediumUrl else photo.mediumUrl != null)
            return false
        if (if (smallUrl != null) smallUrl != photo.smallUrl else photo.smallUrl != null)
            return false
        return if (thumbnailUrl != null) thumbnailUrl == photo.thumbnailUrl else photo.thumbnailUrl == null
    }

    override fun hashCode(): Int {
        var result = if (originUrl != null) originUrl!!.hashCode() else 0
        result = 31 * result + if (largeUrl != null) largeUrl!!.hashCode() else 0
        result = 31 * result + if (mediumUrl != null) mediumUrl!!.hashCode() else 0
        result = 31 * result + if (smallUrl != null) smallUrl!!.hashCode() else 0
        result = 31 * result + if (thumbnailUrl != null) thumbnailUrl!!.hashCode() else 0
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        return "Photo{" +
                "originUrl='" + originUrl + '\''.toString() +
                ", largeUrl='" + largeUrl + '\''.toString() +
                ", mediumUrl='" + mediumUrl + '\''.toString() +
                ", smallUrl='" + smallUrl + '\''.toString() +
                ", thumbnailUrl='" + thumbnailUrl + '\''.toString() +
                ", width=" + width +
                ", height=" + height +
                '}'.toString()
    }


    fun toMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["originUrl"] = originUrl
        map["largeUrl"] = largeUrl
        map["mediumUrl"] = mediumUrl
        map["smallUrl"] = smallUrl
        map["thumbnailUrl"] = thumbnailUrl
        map["width"] = width
        map["height"] = height
        return map
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(originUrl)
        parcel.writeString(largeUrl)
        parcel.writeString(mediumUrl)
        parcel.writeString(smallUrl)
        parcel.writeString(thumbnailUrl)
        parcel.writeInt(width)
        parcel.writeInt(height)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Photo> {
        override fun createFromParcel(parcel: Parcel): Photo {
            return Photo(parcel)
        }

        override fun newArray(size: Int): Array<Photo?> {
            return arrayOfNulls(size)
        }
    }

}
