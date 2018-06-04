package com.petnbu.petnbu.di

import android.content.SharedPreferences

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.google.gson.Gson
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.feed.CreateEditFeedViewModel
import com.petnbu.petnbu.feed.FeedsViewModel
import com.petnbu.petnbu.feed.comment.CommentsViewModel
import com.petnbu.petnbu.jobs.CreateCommentWorker
import com.petnbu.petnbu.jobs.CreateEditFeedWorker
import com.petnbu.petnbu.login.LoginJavaActivity
import com.petnbu.petnbu.notification.NotificationViewModel
import com.petnbu.petnbu.repo.FeedRepository
import com.petnbu.petnbu.repo.NotificationRepository
import com.petnbu.petnbu.repo.UserRepository
import com.petnbu.petnbu.userprofile.UserProfileViewModel

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = [(AppModule::class), (WebServiceModule::class)])
interface AppComponent {

    val petDb: PetDb

    val feedRepo: FeedRepository

    val userRepo: UserRepository

    val notificationRepo: NotificationRepository

    val appExecutor: AppExecutors

    val jobDispatcher: FirebaseJobDispatcher

    val sharedPref: SharedPreferences

    val gson: Gson

    fun inject(activity: LoginJavaActivity)

    fun inject(viewModel: FeedsViewModel)

    fun inject(createEditFeedWorker: CreateEditFeedWorker)

    fun inject(viewModel: CreateEditFeedViewModel)

    fun inject(userProfileViewModel: UserProfileViewModel)

    fun inject(commentsViewModel: CommentsViewModel)

    fun inject(notificationViewModel: NotificationViewModel)

    fun inject(createCommentWorker: CreateCommentWorker)
}
