package com.petnbu.petnbu.api

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import com.google.firebase.firestore.*
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.SharedPrefUtil
import com.petnbu.petnbu.model.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class FirebaseService @Inject
constructor(private val mDb: FirebaseFirestore, private val mExecutors: AppExecutors) : WebService {

    val allUser: LiveData<ApiResponse<List<UserEntity>>>
        get() {
            val result = MutableLiveData<ApiResponse<List<UserEntity>>>()
            val users = ArrayList<UserEntity>()
            mDb.collection(USERS).get()
                    .addOnSuccessListener { documentsSnapshot ->
                        mExecutors.networkIO().execute {
                            for (user in documentsSnapshot) {
                                users.add(user.toObject(UserEntity::class.java))
                            }
                            result.setValue(ApiResponse(users, true, null))
                        }
                    }
                    .addOnFailureListener { e ->
                        result.setValue(ApiResponse(e))
                    }

            return result
        }

    override fun createFeed(feed: Feed): LiveData<ApiResponse<Feed>> {
        val result = MutableLiveData<ApiResponse<Feed>>()

        val batch = mDb.batch()

        val doc = mDb.collection(GLOBAL_FEEDS).document()
        val oldId = feed.feedId
        feed.feedId = doc.id
        feed.timeCreated = null
        feed.timeUpdated = null
        batch.set(doc, feed)
        val userFeed = mDb.collection(USERS)
                .document(feed.feedUser.userId)
                .collection(FEEDS).document(feed.feedId)
        batch.set(userFeed, feed)
        batch.commit()
                .addOnSuccessListener {
                    feed.timeCreated = Date()
                    feed.timeUpdated = Date()
                    result.setValue(ApiResponse(feed, true, null))
                }
                .addOnFailureListener { e ->
                    feed.feedId = oldId
                    result.setValue(ApiResponse(e))
                }
        return result
    }

    override fun updateFeed(feed: Feed): LiveData<ApiResponse<Feed>> {
        val result = MutableLiveData<ApiResponse<Feed>>()
        if (feed.feedUser == null) {
            result.value = ApiResponse(null, false,
                    "to update Feed. It is required feedId, feedUser, feedUserId must not null!")
            return result
        }

        val photosMap = ArrayList<Map<String, Any>>()
        for (photo in feed.photos) {
            photosMap.add(photo.toMap())
        }

        val updates: Map<String, Any> = mapOf("content" to feed.content, "photos" to photosMap)

        val batch = mDb.batch()
        val doc = mDb.collection(GLOBAL_FEEDS).document(feed.feedId)
        feed.timeUpdated = null
        batch.update(doc, updates)
        val userFeed = mDb.document(String.format("users/%s/feeds/%s", feed.feedUser.userId, feed.feedId))
        batch.update(userFeed, updates)
        batch.commit()
                .addOnSuccessListener {
                    mExecutors.networkIO().execute {
                        feed.timeUpdated = Date()
                        result.postValue(ApiResponse(feed, true, null))
                    }
                }
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return result
    }


    override fun getGlobalFeeds(after: Long, limit: Int): LiveData<ApiResponse<List<Feed>>> {
        val result = MutableLiveData<ApiResponse<List<Feed>>>()
        val dateAfter = Date(after)
        mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                .limit(limit.toLong()).startAfter(dateAfter)
                .get()
                .addOnSuccessListener { queryDocumentSnapshots ->
                    mExecutors.networkIO().execute {
                        val feedResponse = ArrayList<Feed>(limit)
                        for (doc in queryDocumentSnapshots.documents) {
                            Timber.i(doc.toString())
                            doc.toObject(Feed::class.java)?.run { feedResponse.add(this) }
                        }
                        Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.documents.size)
                        result.postValue(ApiResponse(feedResponse, true, null))
                    }
                }.addOnFailureListener { e ->
                    result.setValue(ApiResponse(e))
                }

        return processUserLikeFeeds(result, SharedPrefUtil.userId)
    }

    override fun getGlobalFeeds(afterFeedId: String, limit: Int): LiveData<ApiResponse<List<Feed>>> {
        val result = MutableLiveData<ApiResponse<List<Feed>>>()
        val feedDoc = mDb.collection(GLOBAL_FEEDS).document(afterFeedId)
        feedDoc.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                        .startAfter(documentSnapshot)
                        .get()
                        .addOnSuccessListener { queryDocumentSnapshots ->
                            mExecutors.networkIO().execute {
                                val feedResponse = ArrayList<Feed>(limit)
                                queryDocumentSnapshots.documents.forEach {
                                    it.toObject(Feed::class.java)?.run { feedResponse.add(this) }
                                }
                                Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.documents.size)
                                result.postValue(ApiResponse(feedResponse, true, null))
                            }
                        }.addOnFailureListener { e ->
                            result.setValue(ApiResponse(e))
                        }
            } else {
                result.setValue(ApiResponse(null, false, "feedId not exists"))
            }

        }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }

        return processUserLikeFeeds(result, SharedPrefUtil.userId)
    }


    override fun getUserFeed(userId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Feed>>> {
        val result = MutableLiveData<ApiResponse<List<Feed>>>()
        mDb.collection(USERS).document(userId).collection(FEEDS)
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .startAfter(Date(after))
                .limit(limit.toLong())
                .get()
                .addOnSuccessListener { queryDocumentSnapshots ->
                    mExecutors.networkIO().execute {
                        val feedResponse = ArrayList<Feed>(limit)
                        for (doc in queryDocumentSnapshots) {
                            Timber.i("Feed: %s", doc.toString())
                            feedResponse.add(doc.toObject(Feed::class.java))
                        }
                        result.postValue(ApiResponse(feedResponse, true, null))
                    }
                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return processUserLikeFeeds(result, userId)
    }

    override fun getUserFeed(userId: String, afterFeedId: String, limit: Int): LiveData<ApiResponse<List<Feed>>> {
        val result = MutableLiveData<ApiResponse<List<Feed>>>()
        mDb.collection(GLOBAL_FEEDS).document(afterFeedId).get()
                .addOnSuccessListener { documentSnapshot ->
                    mExecutors.networkIO().execute {
                        if (documentSnapshot.exists()) {
                            mDb.collection(USERS).document(userId).collection(FEEDS)
                                    .orderBy("timeCreated", Query.Direction.DESCENDING)
                                    .startAfter(documentSnapshot)
                                    .limit(limit.toLong())
                                    .get()
                                    .addOnSuccessListener { queryDocumentSnapshots ->
                                        val feedResponse = ArrayList<Feed>(limit)
                                        for (doc in queryDocumentSnapshots) {
                                            feedResponse.add(doc.toObject(Feed::class.java))
                                        }
                                        result.postValue(ApiResponse(feedResponse, true, null))
                                    }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
                        } else {
                            result.setValue(ApiResponse(null, false, "feedId $afterFeedId does not exist"))
                        }

                    }
                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }

        return processUserLikeFeeds(result, userId)
    }

    override fun getFeed(feedId: String): LiveData<ApiResponse<Feed>> {
        val result = MutableLiveData<ApiResponse<Feed>>()
        mDb.collection(GLOBAL_FEEDS).document(feedId)
                .get()
                .addOnSuccessListener { queryDocumentSnapshots ->
                    mExecutors.networkIO().execute {
                        val feed = queryDocumentSnapshots.toObject(Feed::class.java)
                        Timber.i("onSuccess: loaded %s feed(s)", feed)
                        result.setValue(ApiResponse(feed, true, null))
                    }
                }.addOnFailureListener { e ->
                    Timber.e("onFailure: %s", e.message)
                    result.setValue(ApiResponse(e))
                }
        return result
    }

    override fun likeFeed(userId: String, feedId: String): LiveData<ApiResponse<Feed>> {
        val result = MutableLiveData<ApiResponse<Feed>>()
        mDb.document("users/$userId").get()
                .addOnSuccessListener { fromUserSnap ->
                    mDb.runTransaction { transaction ->
                        Timber.i("like feed transaction")
                        val transactionResult: ApiResponse<Feed>
                        val feed = transaction.get(mDb.document("global_feeds/$feedId")).toObject(Feed::class.java)
                                ?: throw FirebaseFirestoreException("Feed not found", FirebaseFirestoreException.Code.NOT_FOUND)

                        val updates = HashMap<String, Any>()
                        val likeByUsers = mDb.collection("global_feeds").document(feedId)
                                .collection("likedByUsers").document(userId)
                        var newLikeCount = feed.likeCount + 1
                        if (transaction.get(likeByUsers).exists()) {//user already like this feed
                            Timber.i("user already like this feed")
                            newLikeCount--
                        }
                        val timeStamp = HashMap<String, Any>()
                        timeStamp["timeCreated"] = FieldValue.serverTimestamp()
                        transaction.set(likeByUsers, timeStamp)
                        val userLikePosts = mDb.collection("users").document(userId)
                                .collection("likePosts").document(feedId)
                        transaction.set(userLikePosts, timeStamp)
                        updates["likeCount"] = newLikeCount
                        updateFeedTransaction(transaction, feed, updates)

                        val notification = Notification()
                        notification.fromUser = fromUserSnap.toObject(FeedUser::class.java)
                        notification.type = Notification.TYPE_LIKE_FEED
                        notification.targetUserId = feed.feedUser.userId
                        notification.targetFeedId = feedId
                        createNotificationInTransaction(transaction, notification)

                        feed.isLiked = true
                        feed.likeCount = newLikeCount
                        transactionResult = ApiResponse(feed, true, null)
                        transactionResult
                    }.addOnSuccessListener({ result.setValue(it) })
                            .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
                }
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }

        return result
    }

    override fun unLikeFeed(userId: String, feedId: String): LiveData<ApiResponse<Feed>> {
        val result = MutableLiveData<ApiResponse<Feed>>()
        mDb.runTransaction { transaction ->
            val transactionResult: ApiResponse<Feed>
            val feed = transaction.get(mDb.document("global_feeds/$feedId")).toObject(Feed::class.java)
                    ?: throw FirebaseFirestoreException("Feed not found", FirebaseFirestoreException.Code.NOT_FOUND)

            val likeByUsers = mDb.collection("global_feeds").document(feedId)
                    .collection("likedByUsers").document(userId)


            val updates = HashMap<String, Any>()
            var newLikeCount = feed.likeCount - 1
            if (!transaction.get(likeByUsers).exists()) { // user already unlike this feed
                newLikeCount++
            }

            transaction.delete(likeByUsers)
            val userLikePosts = mDb.collection("users").document(userId)
                    .collection("likePosts").document(feedId)
            transaction.delete(userLikePosts)

            if (newLikeCount < 0) {
                throw FirebaseFirestoreException("unlike should never cause like count less than zero",
                        FirebaseFirestoreException.Code.OUT_OF_RANGE)
            }
            updates["likeCount"] = newLikeCount
            updateFeedTransaction(transaction, feed, updates)
            feed.isLiked = false
            feed.likeCount = newLikeCount

            transactionResult = ApiResponse(feed, true, null)
            transactionResult
        }.addOnSuccessListener({ result.setValue(it) }).addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return result
    }

    override fun likeComment(userId: String, commentId: String): LiveData<ApiResponse<Comment>> {
        val result = MutableLiveData<ApiResponse<Comment>>()
        mDb.document("users/$userId").get().addOnSuccessListener { userSnap ->
            mDb.runTransaction { transaction ->
                Timber.i("like comment transaction")
                val transactionResult: ApiResponse<Comment>

                val comment = transaction.get(mDb.document("comments/$commentId")).toObject(Comment::class.java)
                        ?: throw FirebaseFirestoreException("comment not found", FirebaseFirestoreException.Code.NOT_FOUND)

                val feedContainerRef = mDb.document("global_feeds/" + comment.parentFeedId)
                val feedContainer = transaction.get(feedContainerRef).toObject(Feed::class.java)
                        ?: throw FirebaseFirestoreException("the feed contain this comment is not found", FirebaseFirestoreException.Code.NOT_FOUND)

                val updates = HashMap<String, Any>()
                val likeByUsers = mDb.collection("comments").document(commentId).collection("likedByUsers").document(userId)
                var newLikeCount = comment.likeCount + 1
                if (transaction.get(likeByUsers).exists()) {//user already like this comment
                    Timber.i("user already like this comment")
                    newLikeCount--
                }
                val timeStamp = HashMap<String, Any>()
                timeStamp["timeCreated"] = FieldValue.serverTimestamp()
                transaction.set(likeByUsers, timeStamp)

                val userLikePosts = mDb.collection("users").document(userId).collection("likeComments").document(commentId)
                transaction.set(userLikePosts, timeStamp)
                updates["likeCount"] = newLikeCount
                updateCommentTransaction(transaction, comment, updates)

                transaction.set(feedContainerRef, feedContainer)

                val notification = Notification()
                notification.fromUser = userSnap.toObject(FeedUser::class.java)
                notification.type = Notification.TYPE_LIKE_COMMENT
                notification.targetUserId = comment.feedUser.userId
                notification.targetCommentId = comment.id
                notification.targetFeedId = feedContainer.feedId
                createNotificationInTransaction(transaction, notification)

                comment.isLiked = true
                comment.likeCount = newLikeCount
                transactionResult = ApiResponse(comment, true, null)
                transactionResult
            }.addOnSuccessListener({ result.setValue(it) })
                    .addOnFailureListener {
                        e -> result.setValue(ApiResponse(e))
                    }
                    .addOnFailureListener {
                        e -> result.setValue(ApiResponse(e))
                    }
        }
        return result
    }

    override fun unLikeComment(userId: String, commentId: String): LiveData<ApiResponse<Comment>> {
        val result = MutableLiveData<ApiResponse<Comment>>()
        mDb.runTransaction { transaction ->
            Timber.i("like comment transaction")
            val transactionResult: ApiResponse<Comment>

            val comment = transaction.get(mDb.document("comments/$commentId")).toObject(Comment::class.java)
                    ?: throw FirebaseFirestoreException("comment not found", FirebaseFirestoreException.Code.NOT_FOUND)

            val feedContainerRef = mDb.document("global_feeds/" + comment.parentFeedId)
            val feedContainer = transaction.get(feedContainerRef).toObject(Feed::class.java)
                    ?: throw FirebaseFirestoreException("the feed contain this comment is not found", FirebaseFirestoreException.Code.NOT_FOUND)

            val updates = HashMap<String, Any>()
            val likeByUsers = mDb.collection("comments").document(commentId).collection("likedByUsers").document(userId)
            var newLikeCount = comment.likeCount - 1
            if (!transaction.get(likeByUsers).exists()) {//user already like this comment
                Timber.i("user did not like this comment yet")
                newLikeCount++
            }
            transaction.delete(likeByUsers)

            val userLikePosts = mDb.collection("users").document(userId).collection("likeComments").document(commentId)
            transaction.delete(userLikePosts)
            updates["likeCount"] = newLikeCount
            updateCommentTransaction(transaction, comment, updates)

            transaction.set(feedContainerRef, feedContainer)

            comment.isLiked = false
            comment.likeCount = newLikeCount
            transactionResult = ApiResponse(comment, true, null)
            transactionResult
        }.addOnSuccessListener({ result.setValue(it) })
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return result
    }

    override fun likeSubComment(userId: String, subCommentId: String): LiveData<ApiResponse<Comment>> {
        val result = MutableLiveData<ApiResponse<Comment>>()
        mDb.document("users/$userId").get().addOnSuccessListener { userSnap ->
            mDb.runTransaction { transaction ->
                val subCommentRef = mDb.document(String.format("subComments/%s", subCommentId))
                val subComment = transaction.get(subCommentRef).toObject(Comment::class.java)
                        ?: throw FirebaseFirestoreException("the comment $subCommentId does not found",
                                FirebaseFirestoreException.Code.NOT_FOUND)

                val likeByUsers = mDb.collection("subComments").document(subCommentId).collection("likedByUsers").document(userId)
                var newLikeCount = subComment.likeCount + 1
                if (transaction.get(likeByUsers).exists()) {//user already like this comment
                    Timber.i("user did not like this comment yet")
                    newLikeCount--
                }
                val timeStamp = HashMap<String, Any>()
                timeStamp["timeCreated"] = FieldValue.serverTimestamp()
                transaction.set(likeByUsers, timeStamp)
                val userLikePosts = mDb.collection("users").document(userId).collection("likeSubComments").document(subCommentId)
                transaction.set(userLikePosts, timeStamp)
                val likeCountUpdate = HashMap<String, Any>()
                likeCountUpdate["likeCount"] = newLikeCount

                updateSubCommentTransaction(transaction, subComment, likeCountUpdate)

                val notification = Notification()
                notification.targetReplyId = subCommentId
                notification.targetUserId = subComment.feedUser.userId
                notification.fromUser = userSnap.toObject(FeedUser::class.java)
                notification.type = Notification.TYPE_LIKE_REPLY
                notification.targetCommentId = subComment.parentCommentId
                createNotificationInTransaction(transaction, notification)

                subComment.isLiked = true
                subComment.likeCount = newLikeCount
                ApiResponse(subComment, true, null)
            }.addOnSuccessListener({ result.setValue(it) })
                    .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        }
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return result
    }

    override fun unLikeSubComment(userId: String, subCommentId: String): LiveData<ApiResponse<Comment>> {
        val result = MutableLiveData<ApiResponse<Comment>>()
        mDb.runTransaction { transaction ->
            val subCommentRef = mDb.document(String.format("subComments/%s", subCommentId))
            val subComment = transaction.get(subCommentRef).toObject(Comment::class.java)
                    ?: throw FirebaseFirestoreException("the comment $subCommentId does not found",
                            FirebaseFirestoreException.Code.NOT_FOUND)

            val likeByUsers = mDb.collection("subComments").document(subCommentId).collection("likedByUsers").document(userId)
            var newLikeCount = subComment.likeCount - 1
            if (!transaction.get(likeByUsers).exists()) {//user already like this comment
                Timber.i("user did not like this comment yet")
                newLikeCount++
            }
            transaction.delete(likeByUsers)
            val userLikePosts = mDb.collection("users").document(userId).collection("likeSubComments").document(subCommentId)
            transaction.delete(userLikePosts)
            val likeCountUpdate = HashMap<String, Any>()
            likeCountUpdate["likeCount"] = newLikeCount

            updateSubCommentTransaction(transaction, subComment, likeCountUpdate)

            subComment.isLiked = false
            subComment.likeCount = newLikeCount
            ApiResponse(subComment, true, null)
        }.addOnSuccessListener({ result.setValue(it) })
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return result
    }

    override fun createUser(userEntity: UserEntity): LiveData<ApiResponse<UserEntity>> {
        val result = MutableLiveData<ApiResponse<UserEntity>>()
        val userDoc = mDb.collection(USERS).document(userEntity.userId)
        userDoc.set(userEntity)
                .addOnSuccessListener { result.setValue(ApiResponse(userEntity, true, null)) }
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return result
    }

    override fun getUser(userId: String): LiveData<ApiResponse<UserEntity>> {

        val result = MutableLiveData<ApiResponse<UserEntity>>()

        mDb.collection(USERS).document(userId).get()
                .addOnSuccessListener { documentSnapshot ->
                    mExecutors.networkIO().execute {
                        if (documentSnapshot.exists()) {
                            val userEntity = documentSnapshot.toObject(UserEntity::class.java)
                            result.postValue(ApiResponse(userEntity, true, null))
                        } else {
                            result.postValue(ApiResponse(null, false, "User not found"))
                        }
                    }
                }
                .addOnFailureListener { e -> result.setValue(ApiResponse(e)) }

        return result
    }

    override fun updateUser(userEntity: UserEntity, callback: SuccessCallback<Void>) {
        val userDoc = mDb.collection(USERS).document()
        userEntity.userId = userDoc.id
        userDoc.set(userEntity, SetOptions.merge())
                .addOnSuccessListener({ callback.onSuccess(it) })
                .addOnFailureListener({ callback.onFailed(it) })
    }

    override fun createFeedComment(comment: Comment, feedId: String): LiveData<ApiResponse<Comment>> {
        val result = MutableLiveData<ApiResponse<Comment>>()
        val oldId = comment.id
        mDb.runTransaction { transaction ->
            val feedRef = mDb.collection(GLOBAL_FEEDS).document(feedId)
            val feed = transaction.get(feedRef).toObject(Feed::class.java)
                    ?: throw FirebaseFirestoreException(String.format("feed Id %s does not found", feedId),
                            FirebaseFirestoreException.Code.NOT_FOUND)

            val newCommentCount = (feed.commentCount + 1).toDouble()
            val commentRef = mDb.collection("comments").document()
            comment.id = commentRef.id

            val commentMap = comment.toMap()
            commentMap["timeCreated"] = FieldValue.serverTimestamp()
            commentMap["timeUpdated"] = FieldValue.serverTimestamp()


            //update commentCount
            val commentCountUpdates = HashMap<String, Any>()
            commentCountUpdates["commentCount"] = newCommentCount
            updateFeedTransaction(transaction, feed, commentCountUpdates)

            //update Feed latestComment
            val latestCommentUpdates = HashMap<String, Any>()
            latestCommentUpdates["latestComment"] = commentMap
            updateFeedTransaction(transaction, feed, latestCommentUpdates)

            val notification = Notification()
            notification.type = Notification.TYPE_NEW_COMMENT
            notification.targetUserId = feed.feedUser.userId
            notification.targetFeedId = feed.feedId
            notification.targetCommentId = comment.id
            notification.fromUser = comment.feedUser
            createNotificationInTransaction(transaction, notification)

            transaction.set(commentRef, commentMap)

            val userFeedCommentPath = "users/${feed.feedUser.userId}/feeds/${feed.feedId}/comments/${comment.id}"
            val userFeedCommentRef = mDb.document(userFeedCommentPath)
            transaction.set(userFeedCommentRef, commentMap)

            val feedCommentPath = "global_feeds/$feedId/comments/${comment.id}"
            val feedCommentRef = mDb.document(feedCommentPath)
            transaction.set(feedCommentRef, commentMap)
            val transResult = ApiResponse(comment, true, null)
            transResult
        }.addOnSuccessListener({ result.setValue(it) }).addOnFailureListener { e ->
            comment.id = oldId
            result.setValue(ApiResponse(e))
        }
        return result
    }

    override fun createReplyComment(subComment: Comment, parentCommentId: String): LiveData<ApiResponse<Comment>> {
        val result = MutableLiveData<ApiResponse<Comment>>()
        val oldId = subComment.id
        mDb.runTransaction { transaction ->
            val parentCommentRef = mDb.document(String.format("comments/%s", parentCommentId))
            val parentComment = transaction.get(parentCommentRef).toObject(Comment::class.java)
                    ?: throw FirebaseFirestoreException("the parentComment $parentCommentId does not found",
                            FirebaseFirestoreException.Code.NOT_FOUND)

            if (parentComment.parentFeedId == null) {
                throw FirebaseFirestoreException("this comment missing parent feed id",
                        FirebaseFirestoreException.Code.DATA_LOSS)
            }

            val feedContainerRef = mDb.document("global_feeds/" + parentComment.parentFeedId)
            val feedContainer = transaction.get(feedContainerRef).toObject(Feed::class.java)
                    ?: throw FirebaseFirestoreException("the feed you're trying to comment does not exist",
                            FirebaseFirestoreException.Code.NOT_FOUND)

            val newCommentCount = (parentComment.commentCount + 1).toDouble()
            val subCommentRef = mDb.collection("subComments").document()
            subComment.id = subCommentRef.id
            val commentMap = subComment.toMap()
            commentMap["timeCreated"] = FieldValue.serverTimestamp()
            commentMap["timeUpdated"] = FieldValue.serverTimestamp()


            val updatesCount = HashMap<String, Any>()
            updatesCount["commentCount"] = newCommentCount

            val latestCommentUpdate = HashMap<String, Any>()
            latestCommentUpdate["latestComment"] = commentMap


            //                update feed subComment count
            this@FirebaseService.updateFeedTransaction(transaction, feedContainer, updatesCount)
            //                update parent's subComment count
            this@FirebaseService.updateCommentTransaction(transaction, parentComment, updatesCount)
            this@FirebaseService.updateCommentTransaction(transaction, parentComment, latestCommentUpdate)

            transaction.set(subCommentRef, commentMap)
            val replyCommentPath = String.format("comments/%s/subComments/%s", parentCommentId, subComment.id)
            val replyCommentRef = mDb.document(replyCommentPath)
            transaction.set(replyCommentRef, commentMap)

            val notification = Notification()
            notification.fromUser = subComment.feedUser
            notification.targetCommentId = subComment.parentCommentId
            notification.targetUserId = parentComment.feedUser.userId
            notification.type = Notification.TYPE_NEW_REPLY
            createNotificationInTransaction(transaction, notification)

            ApiResponse(subComment, true, null)

        }.addOnSuccessListener({ result.setValue(it) }).addOnFailureListener { e ->
            subComment.id = oldId
            result.setValue(ApiResponse(e))
        }
        return result
    }

    override fun getFeedComments(feedId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Comment>>> {
        val result = MutableLiveData<ApiResponse<List<Comment>>>()
        val feedCommentsPath = String.format("global_feeds/%s/comments", feedId)
        val ref = mDb.collection(feedCommentsPath)
        ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(Date(after)).limit(limit.toLong())
                .get().addOnSuccessListener { queryDocumentSnapshots ->
                    mExecutors.networkIO().execute {
                        Timber.i("getFeedComments succeed")
                        val comments = ArrayList<Comment>()
                        for (cmtSnapShot in queryDocumentSnapshots) {
                            comments.add(cmtSnapShot.toObject(Comment::class.java))
                        }
                        result.postValue(ApiResponse(comments, true, null))
                    }
                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return processUserLikeComment(false, result, SharedPrefUtil.userId)
    }

    override fun getCommentsPaging(feedId: String, commentId: String, limit: Int): LiveData<ApiResponse<List<Comment>>> {
        val result = MutableLiveData<ApiResponse<List<Comment>>>()
        mDb.document(String.format("global_feeds/%s/comments/%s", feedId, commentId))
                .get().addOnSuccessListener { documentSnapshot ->
                    mExecutors.networkIO().execute {
                        if (!documentSnapshot.exists()) {
                            result.postValue(ApiResponse(null, false, "comment not found"))
                            return@execute
                        }
                        val feedCommentsPath = String.format("global_feeds/%s/comments", feedId)
                        val ref = mDb.collection(feedCommentsPath)
                        ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(documentSnapshot).limit(limit.toLong()).get()
                                .addOnSuccessListener { queryDocumentSnapshots ->
                                    mExecutors.networkIO().execute {
                                        Timber.i("getFeedComments succeed")
                                        val comments = ArrayList<Comment>()
                                        for (cmtSnapShot in queryDocumentSnapshots) {
                                            comments.add(cmtSnapShot.toObject(Comment::class.java))
                                        }
                                        result.postValue(ApiResponse(comments, true, null))
                                    }
                                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
                    }
                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }

        return processUserLikeComment(false, result, SharedPrefUtil.userId)
    }

    override fun getSubComments(commentId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Comment>>> {
        val result = MutableLiveData<ApiResponse<List<Comment>>>()
        val subCommentsPath = String.format("comments/%s/subComments", commentId)
        val ref = mDb.collection(subCommentsPath)
        ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(Date(after)).limit(limit.toLong())
                .get().addOnSuccessListener { queryDocumentSnapshots ->
                    mExecutors.networkIO().execute {
                        Timber.i("getFeedComments succeed")
                        val subComments = ArrayList<Comment>()
                        for (cmtSnapShot in queryDocumentSnapshots) {
                            subComments.add(cmtSnapShot.toObject(Comment::class.java))
                        }
                        result.postValue(ApiResponse(subComments, true, null))
                    }
                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
        return processUserLikeComment(true, result, SharedPrefUtil.userId)
    }

    override fun getSubCommentsPaging(commentId: String, afterCommentId: String, limit: Int): LiveData<ApiResponse<List<Comment>>> {
        val result = MutableLiveData<ApiResponse<List<Comment>>>()
        mDb.document(String.format("comments/%s/subComments/%s", commentId, afterCommentId))
                .get().addOnSuccessListener { documentSnapshot ->
                    mExecutors.networkIO().execute {
                        if (!documentSnapshot.exists()) {
                            result.postValue(ApiResponse(null, false, "sub comment not found"))
                            return@execute
                        }
                        val subCommentsPath = String.format("comments/%s/subComments", commentId)
                        val ref = mDb.collection(subCommentsPath)
                        ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(documentSnapshot).limit(limit.toLong())
                                .get().addOnSuccessListener { queryDocumentSnapshots ->
                                    mExecutors.networkIO().execute {
                                        Timber.i("getFeedComments succeed")
                                        val subComments = ArrayList<Comment>()
                                        for (cmtSnapShot in queryDocumentSnapshots) {
                                            subComments.add(cmtSnapShot.toObject(Comment::class.java))
                                        }
                                        result.postValue(ApiResponse(subComments, true, null))
                                    }
                                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }
                    }
                }.addOnFailureListener { e -> result.setValue(ApiResponse(e)) }

        return processUserLikeComment(true, result, SharedPrefUtil.userId)
    }

    private fun updateFeedTransaction(transaction: Transaction, feed: Feed, update: Map<String, Any>) {
        val feedRef = mDb.document("global_feeds/${feed.feedId}")
        val userFeed = mDb.document("users/${feed.feedUser.userId}/feeds/${feed.feedId}")
        transaction.update(feedRef, update)
        transaction.update(userFeed, update)
    }

    private fun updateCommentTransaction(transaction: Transaction, comment: Comment, update: Map<String, Any>) {
        val commentRef = mDb.document("comments/${comment.id}")
        val feedCommentRef = mDb.document("global_feeds/${comment.parentFeedId}/comments/${commentRef.id}")
        transaction.update(commentRef, update)
        transaction.update(feedCommentRef, update)
    }

    private fun updateSubCommentTransaction(transaction: Transaction, subComment: Comment, updates: Map<String, Any>) {
        val subCommentRef = mDb.document("subComments/${subComment.id}")
        val subCommentReply = mDb.document("comments/${subComment.parentCommentId}/subComments/${subComment.id}")
        transaction.update(subCommentRef, updates)
        transaction.update(subCommentReply, updates)
    }

    private fun processUserLikeFeeds(feedsResponse: LiveData<ApiResponse<List<Feed>>>, userId: String): LiveData<ApiResponse<List<Feed>>> {
        return Transformations.switchMap(feedsResponse) { input ->
            val result = MutableLiveData<ApiResponse<List<Feed>>>()
            if (input.isSuccessful) {
                val inputFeeds = input.body
                if (inputFeeds == null || inputFeeds.isEmpty()) {
                    result.value = input
                    return@switchMap result
                }

                val userLikePosts = mDb.collection("users/$userId/likePosts")

                userLikePosts.orderBy("timeCreated").startAt(inputFeeds[inputFeeds.size - 1].timeCreated).get()
                        .addOnSuccessListener { queryDocumentSnapshots ->
                            mExecutors.networkIO().execute {
                                val likedFeeds = HashMap<String, Any>()
                                queryDocumentSnapshots.forEach {
                                    it.get("timeCreated")?.run { likedFeeds[it.id] = this }
                                }
                                inputFeeds.forEach { it.isLiked = (likedFeeds.containsKey(it.feedId)) }
                                result.postValue(ApiResponse(inputFeeds, true, null))
                            }
                        }.addOnFailureListener { e -> result.setValue(ApiResponse(inputFeeds, false, e.message)) }
            } else {
                result.setValue(ApiResponse(null, false, input.errorMessage))
            }
            result
        }
    }

    private fun processUserLikeComment(isSubComment: Boolean,
                                       commentsResponse: LiveData<ApiResponse<List<Comment>>>,
                                       userId: String): LiveData<ApiResponse<List<Comment>>> {
        return Transformations.switchMap(commentsResponse) { input ->
            val result = MutableLiveData<ApiResponse<List<Comment>>>()
            if (input.isSuccessful) {
                val inputFeeds = input.body
                if (inputFeeds == null || inputFeeds.isEmpty()) {
                    result.value = input
                    return@switchMap result
                }

                val userLikePosts = mDb.collection("users/$userId/${if (isSubComment) "likeSubComments" else "likeComments"}")

                userLikePosts.orderBy("timeCreated").startAt(inputFeeds[inputFeeds.size - 1].timeCreated).get()
                        .addOnSuccessListener { queryDocumentSnapshots ->
                            mExecutors.networkIO().execute {
                                val likedComments = HashMap<String, Any>()
                                queryDocumentSnapshots.forEach {
                                    it.get("timeCreated")?.run { likedComments[it.id] = this }
                                }
                                inputFeeds.forEach { it.isLiked = likedComments.containsKey(it.id) }
                                result.postValue(ApiResponse(inputFeeds, true, null))
                            }
                        }.addOnFailureListener { e -> result.setValue(ApiResponse(inputFeeds, false, e.message)) }
            } else {
                result.setValue(ApiResponse(null, false, input.errorMessage))
            }
            result
        }
    }

    private fun createNotificationInTransaction(tran: Transaction, notification: Notification) {
        if (notification.targetUserId == notification.fromUser.userId) {
            Timber.e("target user and from user is the same")
            return
        }
        val notificationIdBuilder = StringBuilder()
        notificationIdBuilder.append(notification.fromUser.userId).append("-")
        when (notification.type) {
            Notification.TYPE_LIKE_COMMENT, Notification.TYPE_NEW_REPLY -> notificationIdBuilder.append(notification.targetCommentId)
            Notification.TYPE_LIKE_FEED, Notification.TYPE_NEW_COMMENT -> notificationIdBuilder.append(notification.targetFeedId)
            Notification.TYPE_LIKE_REPLY -> notificationIdBuilder.append(notification.targetReplyId)
        }
        notificationIdBuilder.append("-").append(notification.type)

        val notificationId = notificationIdBuilder.toString()
        val notificationRef = mDb.document("notifications/$notificationId")
        val userNotificationRef = mDb.document("users/${notification.targetUserId}/notifications/$notificationId")

        notification.id = notificationId
        notification.timeCreated = null
        tran.set(notificationRef, notification)
        tran.set(userNotificationRef, notification)
    }

    override fun getNotifications(userId: String, after: Long, limit: Int): LiveData<ApiResponse<List<Notification>>> {
        val result = MutableLiveData<ApiResponse<List<Notification>>>()
        mDb.collection("users/$userId/notifications")
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .startAfter(Date(after))
                .limit(limit.toLong())
                .get().addOnSuccessListener { querySnapshots ->
                    mExecutors.networkIO().execute {
                        val notifications = ArrayList<Notification>(querySnapshots.size())
                        for (notificationSnap in querySnapshots) {
                            notifications.add(notificationSnap.toObject(Notification::class.java))
                        }
                        result.postValue(ApiResponse(notifications, true, null))
                    }
                }.addOnFailureListener { e -> result.postValue(ApiResponse(e)) }
        return result
    }

    companion object {

        const val GLOBAL_FEEDS = "global_feeds"

        private const val FEEDS = "feeds"

        private const val USERS = "users"
    }

}
