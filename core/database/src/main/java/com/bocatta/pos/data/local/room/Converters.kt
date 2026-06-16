package com.bocatta.pos.data.local.room

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? = value?.let { if (it) 1 else 0 }

    @TypeConverter
    fun toBoolean(value: Int?): Boolean? = value?.let { it == 1 }

    // JSON String passthrough
    @TypeConverter
    fun fromJsonString(value: String?): String? = value

    @TypeConverter
    fun toJsonString(value: String?): String? = value
}