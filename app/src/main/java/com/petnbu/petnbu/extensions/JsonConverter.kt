package com.petnbu.petnbu.extensions

import com.google.gson.Gson
import com.petnbu.petnbu.model.Photo

inline fun Photo.toJson(): String = Gson().toJson(this)

inline fun List<Photo>.toJson(): String = Gson().toJson(this)