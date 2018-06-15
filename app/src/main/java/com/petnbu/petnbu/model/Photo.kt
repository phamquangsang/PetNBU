package com.petnbu.petnbu.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Photo(var originUrl: String,
                 var largeUrl: String? = null,
                 var mediumUrl: String? = null,
                 var smallUrl: String? = null,
                 var thumbnailUrl: String? = null,
                 var width: Int = 0,
                 var height: Int = 0) : Parcelable {

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
