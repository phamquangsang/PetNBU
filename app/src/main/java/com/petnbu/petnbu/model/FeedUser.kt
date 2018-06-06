package com.petnbu.petnbu.model

import android.arch.persistence.room.Ignore
import android.arch.persistence.room.TypeConverters

import com.petnbu.petnbu.db.PhotoConverters

import java.util.HashMap

@TypeConverters(PhotoConverters::class)
data class FeedUser (var userId :String = ""){
    lateinit var avatar: Photo
    var name: String = ""

    constructor():this("") {}

    @Ignore
    constructor(userId: String, avatar: Photo, name: String):this(userId) {
        this.avatar = avatar
        this.name = name
    }

    fun setAvatarUrl(originUrl: String) {
        val photo = Photo(originUrl, null, null, null, 0, 0)
        avatar = photo
    }

    fun toMap(): Map<String, Any> {
        val map = HashMap<String, Any>()
        map["userId"] = userId
        map["avatar"] = avatar.toMap()
        map["name"] = name
        return map
    }

}
