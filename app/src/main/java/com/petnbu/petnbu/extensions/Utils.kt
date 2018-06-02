package com.petnbu.petnbu.extensions

import com.google.gson.Gson

inline fun Any.toJson(): String = Gson().toJson(this)