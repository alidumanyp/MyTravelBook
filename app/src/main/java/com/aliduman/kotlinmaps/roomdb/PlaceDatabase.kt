package com.aliduman.kotlinmaps.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aliduman.kotlinmaps.model.Place

@Database(entities = [Place::class], version = 1) //version özellik değiştirirken, güncelleriz.
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}