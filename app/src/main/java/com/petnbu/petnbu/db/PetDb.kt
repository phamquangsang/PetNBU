package com.petnbu.petnbu.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

import com.petnbu.petnbu.model.CommentEntity
import com.petnbu.petnbu.model.FeedEntity
import com.petnbu.petnbu.model.NotificationEntity
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.UserEntity

@Database(entities = [(UserEntity::class), (FeedEntity::class), (Paging::class), (CommentEntity::class), (NotificationEntity::class)], version = 18)
@TypeConverters(value = [(PetTypeConverters::class)])
abstract class PetDb : RoomDatabase() {

    abstract fun feedDao(): FeedDao

    abstract fun userDao(): UserDao

    abstract fun commentDao(): CommentDao

    abstract fun pagingDao(): PagingDao

    abstract fun notificationDao(): NotificationDao
}
