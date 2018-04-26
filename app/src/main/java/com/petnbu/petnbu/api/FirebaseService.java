package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dagger.Reusable;
import timber.log.Timber;

public class FirebaseService implements WebService {

    public static final String GLOBAL_FEEDS = "global_feeds";

    public static final String USERS = "users";

    private static final String TAG = FirebaseService.class.getSimpleName();

    private FirebaseFirestore mDb;

    public FirebaseService(FirebaseFirestore firebaseFirestore) {
        mDb = firebaseFirestore;
    }

    @Override
    public void createFeed(final Feed feed, final SuccessCallback<Void> callback) {
        DocumentReference doc = mDb.collection(GLOBAL_FEEDS).document();
        feed.setFeedId(doc.getId());
        doc.set(feed)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailed(e));
    }

    @Override
    public void updateFeed(final Feed feed, final SuccessCallback<Void> callback) {
        //set time updated to null so it will get value from server time
        feed.setTimeUpdated(null);
        Log.i(TAG, "updateFeed: time created not null" + (feed.getTimeCreated() != null));
        mDb.collection(GLOBAL_FEEDS).document(feed.getFeedId()).set(feed, SetOptions.merge())
                .addOnFailureListener(e -> callback.onFailed(e))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null));
    }

    @Override
    public void getFeed(String feedId, SuccessCallback<Feed> callback) {
        mDb.collection(GLOBAL_FEEDS).document(feedId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                callback.onSuccess(documentSnapshot.toObject(Feed.class));
            } else {
                callback.onFailed(new IllegalStateException(String.format("Feed Id: %s does not exist", feedId)));
            }
        }).addOnFailureListener(e -> callback.onFailed(e));
    }

    public LiveData<ApiResponse<List<Feed>>> getFeeds(long after, int limit) {
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
                    Timber.e( "onFailure: ", e);
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });

        return result;
    }

    @Override
    public void createUser(User user, SuccessCallback<Void> callback) {
        DocumentReference userDoc = mDb.collection(USERS).document(user.getUserId());
        userDoc.set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess(aVoid))
                .addOnFailureListener(e -> callback.onFailed(e));
    }

    @Override
    public void getUser(String userId, SuccessCallback<User> callback) {
        mDb.collection(USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot.toObject(User.class));
                    } else {
                        callback.onFailed(new IllegalStateException("User not found"));
                    }
                })
                .addOnFailureListener(e -> callback.onFailed(e));
    }

    @Override
    public LiveData<ApiResponse<User>> getUser(String userId) {
        MutableLiveData<ApiResponse<User>> result = new MutableLiveData<>();

        getUser(userId, new SuccessCallback<User>() {
            @Override
            public void onSuccess(User user) {
                result.setValue(new ApiResponse<>(user, true, null));
            }

            @Override
            public void onFailed(Exception e) {
                result.setValue(new ApiResponse<>(null, false, e.getMessage()));
            }
        });
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
}
