package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

public class FirebaseService implements WebService {

    public static final String GLOBAL_FEEDS = "global_feeds";

    private static final String FEEDS = "feeds";

    private static final String USERS = "users";

    private final AppExecutors mExecutors;

    private final FirebaseFirestore mDb;

    @Inject
    public FirebaseService(FirebaseFirestore firebaseFirestore, AppExecutors appExecutors) {
        mDb = firebaseFirestore;
        mExecutors = appExecutors;
    }

    @Override
    public LiveData<ApiResponse<Feed>> createFeed(Feed feed) {
        MutableLiveData<ApiResponse<Feed>> result = new MutableLiveData<>();

        WriteBatch batch = mDb.batch();

        DocumentReference doc = mDb.collection(GLOBAL_FEEDS).document();
        final String oldId = feed.getFeedId();
        feed.setFeedId(doc.getId());
        feed.setTimeCreated(null);
        feed.setTimeUpdated(null);
        batch.set(doc, feed);
        DocumentReference userFeed = mDb.collection(USERS)
                .document(feed.getFeedUser().getUserId())
                .collection(FEEDS).document(feed.getFeedId());
        batch.set(userFeed, feed);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    feed.setTimeCreated(new Date());
                    feed.setTimeUpdated(new Date());
                    result.setValue(new ApiResponse<>(feed, true, null));
                })
                .addOnFailureListener(e -> {
                    feed.setFeedId(oldId);
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });
        return result;
    }

    @Override
    public LiveData<ApiResponse<Feed>> updateFeed(Feed feed) {
        MutableLiveData<ApiResponse<Feed>> result = new MutableLiveData<>();
        if (feed.getFeedUser() == null) {
            result.setValue(new ApiResponse<>(null, false,
                    "to update Feed. It is required feedId, feedUser, feedUserId must not null!"));
            return result;
        }

        List<Map<String, Object>> photosMap = new ArrayList<>();
        for (Photo photo :
                feed.getPhotos()) {
            photosMap.add(photo.toMap());
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("content", feed.getContent());
        updates.put("photos", photosMap);

        WriteBatch batch = mDb.batch();
        DocumentReference doc = mDb.collection(GLOBAL_FEEDS).document(feed.getFeedId());
        feed.setTimeUpdated(null);
        batch.update(doc, updates);
        DocumentReference userFeed =
                mDb.document(String.format("users/%s/feeds/%s", feed.getFeedUser().getUserId(), feed.getFeedId()));
        batch.update(userFeed, updates);
        batch.commit()
                .addOnSuccessListener(aVoid -> mExecutors.networkIO().execute(() -> {
                    feed.setTimeUpdated(new Date());
                    result.postValue(new ApiResponse<>(feed, true, null));
                }))
                .addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }


    public LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(long after, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        Date dateAfter = new Date(after);
        mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                .limit(limit).startAfter(dateAfter)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                    List<Feed> feedRespons = new ArrayList<>(limit);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Timber.i(doc.toString());
                        feedRespons.add(doc.toObject(Feed.class));
                    }
                    Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                    result.postValue(new ApiResponse<>(feedRespons, true, null));
                })).addOnFailureListener(e -> {
            Timber.e("onFailure: %s", e.getMessage());
            result.setValue(new ApiResponse<>(null, false, e.getMessage()));
        });

        return processUserLikeFeeds(result, SharedPrefUtil.getUserId());
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(String afterFeedId, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        DocumentReference feedDoc = mDb.collection(GLOBAL_FEEDS).document(afterFeedId);
        feedDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                        .limit(limit)
                        .startAfter(documentSnapshot)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                            List<Feed> feedRespons = new ArrayList<>(limit);
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                feedRespons.add(doc.toObject(Feed.class));
                            }
                            Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                            result.postValue(new ApiResponse<>(feedRespons, true, null));
                        })).addOnFailureListener(e -> {
                    Timber.e("onFailure: %s", e.getMessage());
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });
            } else {
                result.setValue(new ApiResponse<>(null, false, "feedId not exists"));
            }

        }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return processUserLikeFeeds(result, SharedPrefUtil.getUserId());
    }


    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, long after, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        mDb.collection(USERS).document(userId).collection(FEEDS)
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .startAfter(new Date(after))
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                    List<Feed> feedRespons = new ArrayList<>(limit);
                    for (DocumentSnapshot doc :
                            queryDocumentSnapshots) {
                        Timber.i("Feed: %s", doc.toString());
                        feedRespons.add(doc.toObject(Feed.class));
                    }
                    result.postValue(new ApiResponse<>(feedRespons, true, null));
                })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return processUserLikeFeeds(result, userId);
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, String afterFeedId, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(afterFeedId).get()
                .addOnSuccessListener(documentSnapshot -> mExecutors.networkIO().execute(() -> {
                    if (documentSnapshot.exists()) {
                        mDb.collection(USERS).document(userId).collection(FEEDS)
                                .orderBy("timeCreated", Query.Direction.DESCENDING)
                                .startAfter(documentSnapshot)
                                .limit(limit)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    List<Feed> feedRespons = new ArrayList<>(limit);
                                    for (DocumentSnapshot doc :
                                            queryDocumentSnapshots) {
                                        feedRespons.add(doc.toObject(Feed.class));
                                    }
                                    result.postValue(new ApiResponse<>(feedRespons, true, null));
                                }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
                    } else {
                        result.setValue(new ApiResponse<>(null, false, "feedId " + afterFeedId + " does not exist"));
                    }

                })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return processUserLikeFeeds(result, userId);
    }

    @Override
    public LiveData<ApiResponse<Feed>> getFeed(String feedId) {
        MutableLiveData<ApiResponse<Feed>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(feedId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                    Feed feed = queryDocumentSnapshots.toObject(Feed.class);
                    Timber.i("onSuccess: loaded %s feed(s)", feed);
                    result.setValue(new ApiResponse<>(feed, true, null));
                })).addOnFailureListener(e -> {
            Timber.e("onFailure: %s", e.getMessage());
            result.setValue(new ApiResponse<>(null, false, e.getMessage()));
        });
        return result;
    }

    @Override
    public LiveData<ApiResponse<Feed>> likeFeed(String userId, String feedId) {
        MutableLiveData<ApiResponse<Feed>> result = new MutableLiveData<>();
        mDb.runTransaction(transaction -> {
            Timber.i("like feed transaction");
            ApiResponse<Feed> transactionResult;
            Feed feed = transaction.get(mDb.document("global_feeds/" + feedId)).toObject(Feed.class);
            if (feed == null) {
                throw new FirebaseFirestoreException("Feed not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Map<String, Object> updates = new HashMap<>();
            DocumentReference likeByUsers = mDb.collection("global_feeds").document(feedId).collection("likedByUsers").document(userId);
            int newLikeCount = feed.getLikeCount() + 1;
            if (transaction.get(likeByUsers).exists()) {//user already like this feed
                Timber.i("user already like this feed");
                newLikeCount--;
            }
            Map<String, Object> timeStamp = new HashMap<>();
            timeStamp.put("timeCreated", FieldValue.serverTimestamp());
            transaction.set(likeByUsers, timeStamp);
            DocumentReference userLikePosts = mDb.collection("users").document(userId).collection("likePosts").document(feedId);
            transaction.set(userLikePosts, timeStamp);
            updates.put("likeCount", newLikeCount);
            updateFeedTransaction(transaction, feed, updates);

            feed.setLiked(true);
            feed.setLikeCount(newLikeCount);
            transactionResult = new ApiResponse<>(feed, true, null);
            return transactionResult;
        }).addOnSuccessListener(result::setValue)
                .addOnFailureListener(
                        e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<Feed>> unLikeFeed(String userId, String feedId) {
        MutableLiveData<ApiResponse<Feed>> result = new MutableLiveData<>();
        mDb.runTransaction(transaction -> {
            ApiResponse<Feed> transactionResult;
            Feed feed = transaction.get(mDb.document("global_feeds/" + feedId)).toObject(Feed.class);
            if (feed == null) {
                throw new FirebaseFirestoreException("Feed not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentReference likeByUsers = mDb.collection("global_feeds").document(feedId)
                    .collection("likedByUsers").document(userId);


            Map<String, Object> updates = new HashMap<>();
            int newLikeCount = feed.getLikeCount() - 1;
            if (!transaction.get(likeByUsers).exists()) { // user already unlike this feed
                newLikeCount++;
            }

            transaction.delete(likeByUsers);
            DocumentReference userLikePosts = mDb.collection("users").document(userId)
                    .collection("likePosts").document(feedId);
            transaction.delete(userLikePosts);

            if (newLikeCount < 0) {
                throw new FirebaseFirestoreException("unlike should never cause like count less than zero",
                        FirebaseFirestoreException.Code.OUT_OF_RANGE);
            }
            updates.put("likeCount", newLikeCount);
            updateFeedTransaction(transaction, feed, updates);
            feed.setLiked(false);
            feed.setLikeCount(newLikeCount);

            transactionResult = new ApiResponse<>(feed, true, null);
            return transactionResult;
        }).addOnSuccessListener(result::setValue).addOnFailureListener(
                e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<Comment>> likeComment(String userId, String commentId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        mDb.runTransaction(transaction -> {
            Timber.i("like comment transaction");
            ApiResponse<Comment> transactionResult;

            Comment comment = transaction.get(mDb.document("comments/" + commentId)).toObject(Comment.class);
            if (comment == null) {
                throw new FirebaseFirestoreException("comment not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentReference feedContainerRef = mDb.document("global_feeds/" + comment.getParentFeedId());
            Feed feedContainer = transaction.get(feedContainerRef).toObject(Feed.class);
            if (feedContainer == null) {
                throw new FirebaseFirestoreException("the feed contain this comment is not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Map<String, Object> updates = new HashMap<>();
            DocumentReference likeByUsers = mDb.collection("comments").document(commentId).collection("likedByUsers").document(userId);
            int newLikeCount = comment.getLikeCount() + 1;
            if (transaction.get(likeByUsers).exists()) {//user already like this comment
                Timber.i("user already like this comment");
                newLikeCount--;
            }
            Map<String, Object> timeStamp = new HashMap<>();
            timeStamp.put("timeCreated", FieldValue.serverTimestamp());
            transaction.set(likeByUsers, timeStamp);

            DocumentReference userLikePosts = mDb.collection("users").document(userId).collection("likeComments").document(commentId);
            transaction.set(userLikePosts, timeStamp);
            updates.put("likeCount", newLikeCount);
            updateCommentTransaction(transaction, feedContainer, comment, updates);

            transaction.set(feedContainerRef, feedContainer);

            comment.setLiked(true);
            comment.setLikeCount(newLikeCount);
            transactionResult = new ApiResponse<>(comment, true, null);
            return transactionResult;
        }).addOnSuccessListener(result::setValue)
                .addOnFailureListener(
                        e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<Comment>> unLikeComment(String userId, String commentId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        mDb.runTransaction(transaction -> {
            Timber.i("like comment transaction");
            ApiResponse<Comment> transactionResult;

            Comment comment = transaction.get(mDb.document("comments/" + commentId)).toObject(Comment.class);
            if (comment == null) {
                throw new FirebaseFirestoreException("comment not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentReference feedContainerRef = mDb.document("global_feeds/" + comment.getParentFeedId());
            Feed feedContainer = transaction.get(feedContainerRef).toObject(Feed.class);
            if (feedContainer == null) {
                throw new FirebaseFirestoreException("the feed contain this comment is not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Map<String, Object> updates = new HashMap<>();
            DocumentReference likeByUsers = mDb.collection("comments").document(commentId).collection("likedByUsers").document(userId);
            int newLikeCount = comment.getLikeCount() - 1;
            if (!transaction.get(likeByUsers).exists()) {//user already like this comment
                Timber.i("user did not like this comment yet");
                newLikeCount++;
            }
            Map<String, Object> timeStamp = new HashMap<>();
            timeStamp.put("timeCreated", FieldValue.serverTimestamp());
            transaction.delete(likeByUsers);

            DocumentReference userLikePosts = mDb.collection("users").document(userId).collection("likeComments").document(commentId);
            transaction.delete(userLikePosts);
            updates.put("likeCount", newLikeCount);
            updateCommentTransaction(transaction, feedContainer, comment, updates);

            transaction.set(feedContainerRef, feedContainer);

            comment.setLiked(false);
            comment.setLikeCount(newLikeCount);
            transactionResult = new ApiResponse<>(comment, true, null);
            return transactionResult;
        }).addOnSuccessListener(result::setValue)
                .addOnFailureListener(
                        e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<Comment>> likeSubComment(String userId, String subCommentId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        mDb.runTransaction(transaction -> {
            DocumentReference subCommentRef = mDb.document(String.format("subComments/%s", subCommentId));
            Comment subComment = transaction.get(subCommentRef).toObject(Comment.class);
            if (subComment == null) {
                throw new FirebaseFirestoreException("the comment " + subCommentId + " does not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentReference likeByUsers = mDb.collection("subComments").document(subCommentId).collection("likedByUsers").document(userId);
            int newLikeCount = subComment.getLikeCount() + 1;
            if (transaction.get(likeByUsers).exists()) {//user already like this comment
                Timber.i("user did not like this comment yet");
                newLikeCount--;
            }
            Map<String, Object> timeStamp = new HashMap<>();
            timeStamp.put("timeCreated", FieldValue.serverTimestamp());
            transaction.set(likeByUsers, timeStamp);
            DocumentReference userLikePosts = mDb.collection("users").document(userId).collection("likeSubComments").document(subCommentId);
            transaction.set(userLikePosts, timeStamp);
            HashMap<String, Object> likeCountUpdate = new HashMap<>();
            likeCountUpdate.put("likeCount", newLikeCount);

            updateSubCommentTransaction(transaction, subComment, likeCountUpdate);

            subComment.setLiked(true);
            subComment.setLikeCount(newLikeCount);
            return new ApiResponse<>(subComment, true, null);
        }).addOnSuccessListener(result::setValue)
                .addOnFailureListener(
                        e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<Comment>> unLikeSubComment(String userId, String subCommentId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        mDb.runTransaction(transaction -> {
            DocumentReference subCommentRef = mDb.document(String.format("subComments/%s", subCommentId));
            Comment subComment = transaction.get(subCommentRef).toObject(Comment.class);
            if (subComment == null) {
                throw new FirebaseFirestoreException("the comment " + subCommentId + " does not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentReference likeByUsers = mDb.collection("subComments").document(subCommentId).collection("likedByUsers").document(userId);
            int newLikeCount = subComment.getLikeCount() - 1;
            if (!transaction.get(likeByUsers).exists()) {//user already like this comment
                Timber.i("user did not like this comment yet");
                newLikeCount++;
            }
            Map<String, Object> timeStamp = new HashMap<>();
            timeStamp.put("timeCreated", FieldValue.serverTimestamp());
            transaction.delete(likeByUsers);
            DocumentReference userLikePosts = mDb.collection("users").document(userId).collection("likeSubComments").document(subCommentId);
            transaction.delete(userLikePosts);
            HashMap<String, Object> likeCountUpdate = new HashMap<>();
            likeCountUpdate.put("likeCount", newLikeCount);

            updateSubCommentTransaction(transaction, subComment, likeCountUpdate);

            subComment.setLiked(false);
            subComment.setLikeCount(newLikeCount);
            return new ApiResponse<>(subComment, true, null);
        }).addOnSuccessListener(result::setValue)
                .addOnFailureListener(
                        e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<UserEntity>> createUser(UserEntity userEntity) {
        MutableLiveData<ApiResponse<UserEntity>> result = new MutableLiveData<>();
        DocumentReference userDoc = mDb.collection(USERS).document(userEntity.getUserId());
        userDoc.set(userEntity)
                .addOnSuccessListener(aVoid -> result.setValue(new ApiResponse<>(userEntity, true, null)))
                .addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<UserEntity>> getUser(String userId) {

        MutableLiveData<ApiResponse<UserEntity>> result = new MutableLiveData<>();

        mDb.collection(USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> mExecutors.networkIO().execute(() -> {
                    if (documentSnapshot.exists()) {
                        UserEntity userEntity = documentSnapshot.toObject(UserEntity.class);
                        result.postValue(new ApiResponse<>(userEntity, true, null));
                    } else {
                        result.postValue(new ApiResponse<>(null, false, "User not found"));
                    }
                }))
                .addOnFailureListener(e ->
                        result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    public LiveData<ApiResponse<List<UserEntity>>> getAllUser() {
        MutableLiveData<ApiResponse<List<UserEntity>>> result = new MutableLiveData<>();
        List<UserEntity> users = new ArrayList<>();
        mDb.collection(USERS).get()
                .addOnSuccessListener(documentsSnapshot -> mExecutors.networkIO().execute(() -> {
                    for (DocumentSnapshot user :
                            documentsSnapshot) {
                        users.add(user.toObject(UserEntity.class));
                    }
                    result.setValue(new ApiResponse<>(users, true, null));
                }))
                .addOnFailureListener(e ->
                        result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public void updateUser(UserEntity userEntity, SuccessCallback<Void> callback) {
        DocumentReference userDoc = mDb.collection(USERS).document();
        userEntity.setUserId(userDoc.getId());
        userDoc.set(userEntity, SetOptions.merge())
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onFailed);
    }

    public LiveData<ApiResponse<Comment>> createFeedComment(Comment comment, String feedId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        final String oldId = comment.getId();
        mDb.runTransaction(transaction -> {
            ApiResponse<Comment> transResult;
            DocumentReference feedRef = mDb.collection(GLOBAL_FEEDS).document(feedId);
            Feed feed = transaction.get(feedRef).toObject(Feed.class);
            if (feed == null) {
                throw new FirebaseFirestoreException(String.format("feed Id %s does not found", feedId),
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double newCommentCount = (double) (feed.getCommentCount() + 1);
            DocumentReference commentRef = mDb.collection("comments").document();
            comment.setId(commentRef.getId());

            Map<String, Object> commentMap = comment.toMap();
            commentMap.put("timeCreated", FieldValue.serverTimestamp());
            commentMap.put("timeUpdated", FieldValue.serverTimestamp());


            //update commentCount
            Map<String, Object> commentCountUpdates = new HashMap<>();
            commentCountUpdates.put("commentCount", newCommentCount);
            updateFeedTransaction(transaction, feed, commentCountUpdates);

            //update Feed latestComment
            Map<String, Object> latestCommentUpdates = new HashMap<>();
            latestCommentUpdates.put("latestComment", commentMap);
            updateFeedTransaction(transaction, feed, latestCommentUpdates);


            transaction.set(commentRef, commentMap);

            String userFeedCommentPath = String.format("users/%s/feeds/%s/comments/%s", feed.getFeedUser().getUserId(), feed.getFeedId(), comment.getId());
            DocumentReference userFeedCommentRef = mDb.document(userFeedCommentPath);
            transaction.set(userFeedCommentRef, commentMap);

            String feedCommentPath = String.format("global_feeds/%s/comments/%s", feedId, commentRef.getId());
            DocumentReference feedCommentRef = mDb.document(feedCommentPath);
            transaction.set(feedCommentRef, commentMap);

            transResult = new ApiResponse<>(comment, true, null);
            return transResult;
        }).addOnSuccessListener(result::setValue).addOnFailureListener(e -> {
            comment.setId(oldId);
            result.setValue(new ApiResponse<>(comment, false, e.getMessage()));
        });
        return result;
    }

    @Override
    public LiveData<ApiResponse<Comment>> createReplyComment(Comment subComment, String parentCommentId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        final String oldId = subComment.getId();
        mDb.runTransaction(transaction -> {
            DocumentReference parentCommentRef = mDb.document(String.format("comments/%s", parentCommentId));
            Comment parentComment = transaction.get(parentCommentRef).toObject(Comment.class);
            if (parentComment == null) {
                throw new FirebaseFirestoreException("the parentComment " + parentCommentId + " does not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            if (parentComment.getParentFeedId() == null) {
                throw new FirebaseFirestoreException("this comment missing parent feed id",
                        FirebaseFirestoreException.Code.DATA_LOSS);
            }

            DocumentReference feedContainerRef = mDb.document("global_feeds/" + parentComment.getParentFeedId());
            Feed feedContainer = transaction.get(feedContainerRef).toObject(Feed.class);

            if (feedContainer == null) {
                throw new FirebaseFirestoreException("the feed you're trying to comment does not exist",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Double newCommentCount = (double) (parentComment.getCommentCount() + 1);
            DocumentReference subCommentRef = mDb.collection("subComments").document();
            subComment.setId(subCommentRef.getId());
            Map<String, Object> commentMap = subComment.toMap();
            commentMap.put("timeCreated", FieldValue.serverTimestamp());
            commentMap.put("timeUpdated", FieldValue.serverTimestamp());


            Map<String, Object> updatesCount = new HashMap<>();
            updatesCount.put("commentCount", newCommentCount);

            Map<String, Object> latestCommentUpdate = new HashMap<>();
            latestCommentUpdate.put("latestComment", commentMap);


//                update feed subComment count
            FirebaseService.this.updateFeedTransaction(transaction, feedContainer, updatesCount);
//                update parent's subComment count
            FirebaseService.this.updateCommentTransaction(transaction, feedContainer, parentComment, updatesCount);
            FirebaseService.this.updateCommentTransaction(transaction, feedContainer, parentComment, latestCommentUpdate);


            transaction.set(subCommentRef, commentMap);
            String replyCommentPath = String.format("comments/%s/subComments/%s", parentCommentId, subComment.getId());
            DocumentReference replyCommentRef = mDb.document(replyCommentPath);
            transaction.set(replyCommentRef, commentMap);
            return new ApiResponse<>(subComment, true, null);

        }).addOnSuccessListener(result::setValue).addOnFailureListener(e -> {
            subComment.setId(oldId);
            result.setValue(new ApiResponse<>(subComment, false, e.getMessage()));
        });
        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getFeedComments(String feedId, long after, int limit) {
        MutableLiveData<ApiResponse<List<Comment>>> result = new MutableLiveData<>();
        String feedCommentsPath = String.format("global_feeds/%s/comments", feedId);
        CollectionReference ref = mDb.collection(feedCommentsPath);
        ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(new Date(after)).limit(limit)
                .get().addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
            Timber.i("getFeedComments succeed");
            List<Comment> comments = new ArrayList<>();
            for (DocumentSnapshot cmtSnapShot : queryDocumentSnapshots) {
                comments.add(cmtSnapShot.toObject(Comment.class));
            }
            result.postValue(new ApiResponse<>(comments, true, null));
        })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getCommentsPaging(String feedId, String commentId, int limit) {
        MutableLiveData<ApiResponse<List<Comment>>> result = new MutableLiveData<>();
        mDb.document(String.format("global_feeds/%s/comments/%s", feedId, commentId))
                .get().addOnSuccessListener(documentSnapshot -> mExecutors.networkIO().execute(() -> {
            if (!documentSnapshot.exists()) {
                result.postValue(new ApiResponse<>(null, false, "comment not found"));
                return;
            }
            String feedCommentsPath = String.format("global_feeds/%s/comments", feedId);
            CollectionReference ref = mDb.collection(feedCommentsPath);
            ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(documentSnapshot).limit(limit).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                        Timber.i("getFeedComments succeed");
                        List<Comment> comments = new ArrayList<>();
                        for (DocumentSnapshot cmtSnapShot : queryDocumentSnapshots) {
                            comments.add(cmtSnapShot.toObject(Comment.class));
                        }
                        result.postValue(new ApiResponse<>(comments, true, null));
                    })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getSubComments(String commentId, long after, int limit) {
        MutableLiveData<ApiResponse<List<Comment>>> result = new MutableLiveData<>();
        String subCommentsPath = String.format("comments/%s/subComments", commentId);
        CollectionReference ref = mDb.collection(subCommentsPath);
        ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(new Date(after)).limit(limit)
                .get().addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
            Timber.i("getFeedComments succeed");
            List<Comment> subComments = new ArrayList<>();
            for (DocumentSnapshot cmtSnapShot : queryDocumentSnapshots) {
                subComments.add(cmtSnapShot.toObject(Comment.class));
            }
            result.postValue(new ApiResponse<>(subComments, true, null));
        })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getSubCommentsPaging(String commentId, String afterCommentId, int limit) {
        MutableLiveData<ApiResponse<List<Comment>>> result = new MutableLiveData<>();
        mDb.document(String.format("comments/%s/subComments/%s", commentId, afterCommentId))
                .get().addOnSuccessListener(documentSnapshot -> mExecutors.networkIO().execute(() -> {
            if (!documentSnapshot.exists()) {
                result.postValue(new ApiResponse<>(null, false, "sub comment not found"));
                return;
            }
            String subCommentsPath = String.format("comments/%s/subComments", commentId);
            CollectionReference ref = mDb.collection(subCommentsPath);
            ref.orderBy("timeCreated", Query.Direction.DESCENDING).startAfter(documentSnapshot).limit(limit)
                    .get().addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                Timber.i("getFeedComments succeed");
                List<Comment> subComments = new ArrayList<>();
                for (DocumentSnapshot cmtSnapShot : queryDocumentSnapshots) {
                    subComments.add(cmtSnapShot.toObject(Comment.class));
                }
                result.postValue(new ApiResponse<>(subComments, true, null));
            })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    private void updateFeedTransaction(Transaction transaction, Feed feed, Map<String, Object> update) {
        DocumentReference feedRef = mDb.document(String.format("global_feeds/%s", feed.getFeedId()));
        DocumentReference userFeed = mDb.document(String.format("users/%s/feeds/%s", feed.getFeedUser().getUserId(), feed.getFeedId()));
        transaction.update(feedRef, update);
        transaction.update(userFeed, update);
    }

    private void updateCommentTransaction(Transaction transaction, Feed feedContainer, Comment comment, Map<String, Object> update) {
        DocumentReference commentRef = mDb.document(String.format("comments/%s", comment.getId()));
        DocumentReference feedCommentRef = mDb.document(String.format("global_feeds/%s/comments/%s", comment.getParentFeedId(), commentRef.getId()));
        transaction.update(commentRef, update);
        transaction.update(feedCommentRef, update);
    }

    private void updateSubCommentTransaction(Transaction transaction, Comment subComment, Map<String, Object> updates) {
        DocumentReference subCommentRef = mDb.document(String.format("subComments/%s", subComment.getId()));
        DocumentReference subCommentReply = mDb.document(String.format("comments/%s/subComments/%s", subComment.getParentCommentId(), subComment.getId()));
        transaction.update(subCommentRef, updates);
        transaction.update(subCommentReply, updates);
    }

    private LiveData<ApiResponse<List<Feed>>> processUserLikeFeeds(LiveData<ApiResponse<List<Feed>>> feedsResponse, String userId) {
        return Transformations.switchMap(feedsResponse, input -> {
            MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
            if (input.isSucceed) {
                List<Feed> inputFeeds = input.body;
                if (inputFeeds == null || inputFeeds.isEmpty()) {
                    result.setValue(input);
                    return result;
                }

                CollectionReference userLikePosts = mDb.collection("users").document(userId)
                        .collection("likePosts");

                userLikePosts.orderBy("timeCreated").startAt(inputFeeds.get(inputFeeds.size() - 1).getTimeCreated()).get()
                        .addOnSuccessListener(queryDocumentSnapshots -> mExecutors.networkIO().execute(() -> {
                            Map<String, Object> likedFeeds = new HashMap<>();
                            for (DocumentSnapshot item : queryDocumentSnapshots) {
                                likedFeeds.put(item.getId(), item.get("timeCreated"));
                            }
                            for (Feed feed : inputFeeds) {
                                if (likedFeeds.containsKey(feed.getFeedId())) {
                                    feed.setLiked(true);
                                }
                            }
                            result.postValue(new ApiResponse<>(inputFeeds, true, null));
                        })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(inputFeeds, false, e.getMessage())));
            } else {
                result.setValue(new ApiResponse<>(null, false, input.errorMessage));
            }
            return result;
        });
    }
}
