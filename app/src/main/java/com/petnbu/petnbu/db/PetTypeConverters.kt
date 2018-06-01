package com.petnbu.petnbu.db

import android.arch.persistence.room.TypeConverter
import android.text.TextUtils
import com.google.android.gms.common.util.ArrayUtils
import com.petnbu.petnbu.PetApplication
import java.util.*

public object PetTypeConverters {
    var gson = PetApplication.getAppComponent().gson

    @TypeConverter
    @JvmStatic
    public fun dateToLong(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    @JvmStatic
    public fun longToDate(dateAsLong: Long?): Date? {
        return dateAsLong?.let { Date(it) }
    }

    @TypeConverter
    @JvmStatic
    public fun listStringToString(list: List<String>): String {
        return TextUtils.join(",", list)
    }

    @TypeConverter
    @JvmStatic
    public fun stringBackToList(lists: String): List<String> {
        return ArrayUtils.toArrayList(TextUtils.split(lists, ","))
    }
}
