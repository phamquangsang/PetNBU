package com.petnbu.petnbu.api

import android.arch.lifecycle.LiveData

import com.petnbu.petnbu.model.Comment
import com.petnbu.petnbu.model.Feed
import com.petnbu.petnbu.model.Notification
import com.petnbu.petnbu.model.UserEntity

interface WebService {

    fun createFeed(feed: Feed): LiveData<ApiResponse<Feed>>

    fun updateFeed(feed: Feed): LiveData<ApiResponse<Feed>>

    fun getGlobalFeeds(after: Long, limit: Int): LiveData<ApiResponse<List<Feed>>>

    fun getGlobalFeeds(afterFeedId: String, limit: Int): LiveData<ApiResponse<List<Feed>>>

    fun getUserFeed(userId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Feed>>>

    fun getUserFeed(userId: String, afterFeedId: String, limit: Int): LiveData<ApiResponse<List<Feed>>>

    fun getFeed(feedId: String): LiveData<ApiResponse<Feed>>

    fun createUser(userEntity: UserEntity): LiveData<ApiResponse<UserEntity>>

    fun likeFeed(userId: String, feedId: String): LiveData<ApiResponse<Feed>>

    fun unLikeFeed(userId: String, feedId: String): LiveData<ApiResponse<Feed>>

    fun getUser(userId: String): LiveData<ApiResponse<UserEntity>>

    fun updateUser(userEntity: UserEntity, callback: SuccessCallback<Void>)

    fun getFeedComments(feedId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Comment>>>

    fun getCommentsPaging(feedId: String, commentId: String, limit: Int): LiveData<ApiResponse<List<Comment>>>

    fun createFeedComment(comment: Comment, feedId: String): LiveData<ApiResponse<Comment>>

    fun likeComment(userId: String, commentId: String): LiveData<ApiResponse<Comment>>

    fun unLikeComment(userId: String, commentId: String): LiveData<ApiResponse<Comment>>

    fun likeSubComment(userId: String, subCommentId: String): LiveData<ApiResponse<Comment>>

    fun unLikeSubComment(userId: String, subCommentId: String): LiveData<ApiResponse<Comment>>

    fun createReplyComment(subComment: Comment, parentCommentId: String): LiveData<ApiResponse<Comment>>

    fun getSubComments(commentId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Comment>>>

    fun getSubCommentsPaging(commentId: String, afterCommentId: String, limit: Int): LiveData<ApiResponse<List<Comment>>>

    fun getNotifications(userId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Notification>>>

}


