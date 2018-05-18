package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class FirebaseService implements WebService {

    public static final String GLOBAL_FEEDS = "global_feeds";
    public static final String FEEDS = "feeds";

    public static final String USERS = "users";

    private static final String TAG = FirebaseService.class.getSimpleName();

    private FirebaseFirestore mDb;

    public FirebaseService(FirebaseFirestore firebaseFirestore) {
        mDb = firebaseFirestore;
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
        if (feed.getFeedId() == null || feed.getFeedUser() == null
                || feed.getFeedUser().getUserId() == null) {
            result.setValue(new ApiResponse<>(null, false,
                    "to update Feed. It is required feedId, feedUser, feedUserId must not null!"));
            return result;
        }
        WriteBatch batch = mDb.batch();
        DocumentReference doc = mDb.collection(GLOBAL_FEEDS).document(feed.getFeedId());
        feed.setTimeUpdated(null);
        batch.set(doc, feed);
        DocumentReference userFeed =
                mDb.document(String.format("users/%s/feeds/%s", feed.getFeedUser().getUserId(), feed.getFeedId()));
        batch.set(userFeed, feed);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    feed.setTimeUpdated(new Date());
                    result.setValue(new ApiResponse<>(feed, true, null));
                })
                .addOnFailureListener(e -> {
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });
        return result;
    }


    public LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(long after, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        Date dateAfter = new Date(after);
        mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                .limit(limit).startAfter(dateAfter)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Feed> feedRespons = new ArrayList<>(limit);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        feedRespons.add(doc.toObject(Feed.class));
                    }
                    Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                    result.setValue(new ApiResponse<>(feedRespons, true, null));
                }).addOnFailureListener(e -> {
            Timber.e("onFailure: %s", e.getMessage());
            result.setValue(new ApiResponse<>(null, false, e.getMessage()));
        });

        return result;
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
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<Feed> feedRespons = new ArrayList<>(limit);
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                feedRespons.add(doc.toObject(Feed.class));
                            }
                            Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                            result.setValue(new ApiResponse<>(feedRespons, true, null));
                        }).addOnFailureListener(e -> {
                    Timber.e("onFailure: %s", e.getMessage());
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });
            } else {
                result.setValue(new ApiResponse<>(null, false, "feedId not exists"));
            }

        }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, long after, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        mDb.collection(USERS).document(userId).collection(FEEDS)
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .startAfter(new Date(after))
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Feed> feedRespons = new ArrayList<>(limit);
                    for (DocumentSnapshot doc :
                            queryDocumentSnapshots) {
                        Timber.i("Feed: %s", doc.toString());
                        feedRespons.add(doc.toObject(Feed.class));
                    }
                    result.setValue(new ApiResponse<>(feedRespons, true, null));
                }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, String afterFeedId, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(afterFeedId).get()
                .addOnSuccessListener(documentSnapshot -> {
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
                                    result.setValue(new ApiResponse<>(feedRespons, true, null));
                                }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
                    } else {
                        result.setValue(new ApiResponse<>(null, false, "feedId " + afterFeedId + " does not exist"));
                    }

                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                result.setValue(new ApiResponse<>(null, false, e.getMessage()));
            }
        });

        return result;
    }

    @Override
    public LiveData<ApiResponse<Feed>> getFeed(String feedId) {
        MutableLiveData<ApiResponse<Feed>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(feedId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Feed feed = queryDocumentSnapshots.toObject(Feed.class);
                    Timber.i("onSuccess: loaded %s feed(s)", feed);
                    result.setValue(new ApiResponse<>(feed, true, null));
                }).addOnFailureListener(e -> {
            Timber.e("onFailure: %s", e.getMessage());
            result.setValue(new ApiResponse<>(null, false, e.getMessage()));
        });
        return result;
    }

    @Override
    public LiveData<ApiResponse<Feed>> likeFeed(String feedId) {
        mDb.runTransaction(new Transaction.Function<Feed>() {
            @Nullable
            @Override
            public Feed apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {

                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Feed>() {
            @Override
            public void onSuccess(Feed feed) {

            }
        });
        return null;
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
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserEntity userEntity = documentSnapshot.toObject(UserEntity.class);
                        result.setValue(new ApiResponse<>(userEntity, true, null));
                    } else {
                        result.setValue(new ApiResponse<>(null, false, "User not found"));
                    }
                })
                .addOnFailureListener(e ->
                        result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    public LiveData<ApiResponse<List<UserEntity>>> getAllUser(int limit){
        MutableLiveData<ApiResponse<List<UserEntity>>> result = new MutableLiveData<>();
        List<UserEntity> users = new ArrayList<>();
        mDb.collection(USERS).get()
                .addOnSuccessListener(documentsSnapshot -> {
                    for (DocumentSnapshot user :
                            documentsSnapshot) {
                        users.add(user.toObject(UserEntity.class));
                    }
                    result.setValue(new ApiResponse<>(users, true, null));
                })
                .addOnFailureListener(e ->
                        result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public void updateUser(UserEntity userEntity, SuccessCallback<Void> callback) {
        DocumentReference userDoc = mDb.collection(USERS).document();
        userEntity.setUserId(userDoc.getId());
        userDoc.set(userEntity, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(aVoid))
                .addOnFailureListener(e -> callback.onFailed(e));
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getCommentsPaging(String feedId, String commentId) {
        return null;
    }

    public LiveData<ApiResponse<Comment>> createFeedComment(Comment comment, String feedId) {
        MutableLiveData<ApiResponse<Comment>> result = new MutableLiveData<>();
        final String oldId = comment.getId();
        mDb.runTransaction(new Transaction.Function<Comment>() {
            @Nullable
            @Override
            public Comment apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentReference feedRef = mDb.collection(GLOBAL_FEEDS).document(feedId);
                DocumentSnapshot feed = transaction.get(feedRef);
                if (!feed.exists()) {
                    result.setValue(new ApiResponse<>(null, false, "the feedID " + feedId + " does not found "));
                    return null;
                }

                Double newCommentCount = feed.getDouble("commentCount") + 1;
                DocumentReference commentRef = mDb.collection("comments").document();
                comment.setId(commentRef.getId());

                transaction.update(feedRef, "commentCount", newCommentCount);
                Map<String, Object> commentMap = comment.toMap();
                commentMap.put("timeCreated", FieldValue.serverTimestamp());
                commentMap.put("timeUpdated", FieldValue.serverTimestamp());
                transaction.update(feedRef, "latestComment", commentMap);

                transaction.set(commentRef, commentMap);

                String userCommentPath = String.format("users/%s/comments/%s", comment.getFeedUser().getUserId(), commentRef.getId());
                DocumentReference userCommentRef = mDb.document(userCommentPath);
                transaction.set(userCommentRef, commentMap);

                String feedCommentPath = String.format("global_feeds/%s/comments/%s", feedId, commentRef.getId());
                DocumentReference feedCommentRef = mDb.document(feedCommentPath);
                transaction.set(feedCommentRef, commentMap);

                return comment;
            }
        }).addOnSuccessListener(new OnSuccessListener<Comment>() {
            @Override
            public void onSuccess(Comment comment) {
                result.setValue(new ApiResponse<>(comment, true, null));
            }
        }).addOnFailureListener(e -> {
            comment.setId(oldId);
            result.setValue(new ApiResponse<>(comment, false, e.getMessage()));
        });
        return result;
    }

    @Override
    public LiveData<ApiResponse<Comment>> createReplyComment(Comment comment, String parentCommentId) {
        return null;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getComments(String feedId) {
        return null;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getCommentsByComment(String commentId) {
        return null;
    }
}
