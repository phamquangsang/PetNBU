package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class FirebaseService implements WebService {

    public static final String GLOBAL_FEEDS = "global_feeds";
    public static final String USER_FEEDS = "user_feeds";
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
        feed.setTimeCreated(new Date());
        feed.setTimeUpdated(new Date());
        batch.set(doc, feed);
        DocumentReference userFeed = mDb.collection(USER_FEEDS)
                .document(feed.getFeedUser().getUserId())
                .collection(FEEDS).document(feed.getFeedId());
        batch.set(userFeed, feed);
        batch.commit()
                .addOnSuccessListener(aVoid -> result.setValue(new ApiResponse<>(feed, true, null)))
                .addOnFailureListener(e -> {
                    feed.setFeedId(oldId);
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
                    List<Feed> feeds = new ArrayList<>(limit);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        feeds.add(doc.toObject(Feed.class));
                    }
                    Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                    result.setValue(new ApiResponse<>(feeds, true, null));
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
        feedDoc.get().addOnSuccessListener(documentSnapshot ->
                mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                        .limit(limit)
                        .startAfter(documentSnapshot)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<Feed> feeds = new ArrayList<>(limit);
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                feeds.add(doc.toObject(Feed.class));
                            }
                            Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                            result.setValue(new ApiResponse<>(feeds, true, null));
                        }).addOnFailureListener(e -> {
                    Timber.e("onFailure: %s", e.getMessage());
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, long after, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        mDb.collection(USER_FEEDS).document(userId).collection(FEEDS)
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .startAfter(new Date(after))
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Feed> feeds = new ArrayList<>(limit);
                    for (DocumentSnapshot doc :
                            queryDocumentSnapshots) {
                        Timber.i("Feed: %s", doc.toString());
                        feeds.add(doc.toObject(Feed.class));
                    }
                    result.setValue(new ApiResponse<>(feeds, true, null));
                }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, String afterFeedId, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(afterFeedId).get()
                .addOnSuccessListener(documentSnapshot -> mDb.collection(USER_FEEDS).document(userId).collection(FEEDS)
                        .orderBy("timeCreated", Query.Direction.DESCENDING)
                        .startAfter(documentSnapshot)
                        .limit(limit)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<Feed> feeds = new ArrayList<>(limit);
                            for (DocumentSnapshot doc :
                                    queryDocumentSnapshots) {
                                feeds.add(doc.toObject(Feed.class));
                            }
                            result.setValue(new ApiResponse<>(feeds, true, null));
                        }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())))).addOnFailureListener(new OnFailureListener() {
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
    public LiveData<ApiResponse<User>> createUser(User user) {
        MutableLiveData<ApiResponse<User>> result = new MutableLiveData<>();
        DocumentReference userDoc = mDb.collection(USERS).document(user.getUserId());
        userDoc.set(user)
                .addOnSuccessListener(aVoid -> result.setValue(new ApiResponse<>(user, true, null)))
                .addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<User>> getUser(String userId) {

        MutableLiveData<ApiResponse<User>> result = new MutableLiveData<>();

        mDb.collection(USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        result.setValue(new ApiResponse<>(user, true, null));
                    } else {
                        result.setValue(new ApiResponse<>(null, false, "User not found"));
                    }
                })
                .addOnFailureListener(e ->
                        result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public void updateUser(User user, SuccessCallback<Void> callback) {
        DocumentReference userDoc = mDb.collection(USERS).document();
        user.setUserId(userDoc.getId());
        userDoc.set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(aVoid))
                .addOnFailureListener(e -> callback.onFailed(e));
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
