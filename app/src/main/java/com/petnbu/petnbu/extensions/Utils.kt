package com.petnbu.petnbu.extensions

import com.petnbu.petnbu.PetApplication

fun Any.toJson(): String = PetApplication.appComponent.gson.toJson(this)
