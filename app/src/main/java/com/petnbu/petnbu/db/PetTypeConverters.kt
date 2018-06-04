package com.petnbu.petnbu.db

import android.arch.persistence.room.TypeConverter
import android.text.TextUtils
import com.google.android.gms.common.util.ArrayUtils
import com.google.gson.Gson
import com.petnbu.petnbu.PetApplication
import java.util.*

object PetTypeConverters {
    val gson : Gson = PetApplication.appComponent.gson

    @TypeConverter
    @JvmStatic
    fun dateToLong(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    @JvmStatic
    fun longToDate(dateAsLong: Long?): Date? {
        return dateAsLong?.let { Date(it) }
    }

    @TypeConverter
    @JvmStatic
    fun listStringToString(list: List<String>): String {
        return TextUtils.join(",", list)
    }

    @TypeConverter
    @JvmStatic
    fun stringBackToList(lists: String): List<String> {
        return ArrayUtils.toArrayList(TextUtils.split(lists, ","))
    }
}
