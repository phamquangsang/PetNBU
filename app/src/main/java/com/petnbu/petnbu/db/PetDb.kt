package com.petnbu.petnbu.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import com.petnbu.petnbu.model.*
import java.util.concurrent.Executor

@Database(entities = [(UserEntity::class), (FeedEntity::class), (Paging::class),
    (CommentEntity::class), (NotificationEntity::class)], version = 20)
@TypeConverters(value = [(PetTypeConverters::class)])
abstract class PetDb : RoomDatabase() {

    abstract fun feedDao(): FeedDao

    abstract fun userDao(): UserDao

    abstract fun commentDao(): CommentDao

    abstract fun pagingDao(): PagingDao

    abstract fun notificationDao(): NotificationDao
}

fun RoomDatabase.runInTransaction(executor : Executor, action: () -> Unit) {
    executor.execute {
        runInTransaction {
            action()
        }
    }
}


