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
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    public LiveData<ApiResponse<FeedResponse>> createFeed(FeedResponse feedResponse) {
        MutableLiveData<ApiResponse<FeedResponse>> result = new MutableLiveData<>();

        WriteBatch batch = mDb.batch();

        DocumentReference doc = mDb.collection(GLOBAL_FEEDS).document();
        final String oldId = feedResponse.getFeedId();
        feedResponse.setFeedId(doc.getId());
        feedResponse.setTimeCreated(null);
        feedResponse.setTimeUpdated(null);
        batch.set(doc, feedResponse);
        DocumentReference userFeed = mDb.collection(USERS)
                .document(feedResponse.getFeedUser().getUserId())
                .collection(FEEDS).document(feedResponse.getFeedId());
        batch.set(userFeed, feedResponse);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    feedResponse.setTimeCreated(new Date());
                    feedResponse.setTimeUpdated(new Date());
                    result.setValue(new ApiResponse<>(feedResponse, true, null));
                })
                .addOnFailureListener(e -> {
                    feedResponse.setFeedId(oldId);
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });
        return result;
    }

    @Override
    public LiveData<ApiResponse<FeedResponse>> updateFeed(FeedResponse feedResponse) {
        MutableLiveData<ApiResponse<FeedResponse>> result = new MutableLiveData<>();
        if(feedResponse.getFeedId() == null || feedResponse.getFeedUser() == null
                || feedResponse.getFeedUser().getUserId() == null){
            result.setValue(new ApiResponse<>(null, false,
                    "to update Feed. It is required feedId, feedUser, feedUserId must not null!"));
            return result;
        }
        WriteBatch batch = mDb.batch();
        DocumentReference doc = mDb.collection(GLOBAL_FEEDS).document(feedResponse.getFeedId());
        feedResponse.setTimeUpdated(null);
        batch.set(doc, feedResponse);
        DocumentReference userFeed =
                mDb.document(String.format("users/%s/feeds/%s", feedResponse.getFeedUser().getUserId(), feedResponse.getFeedId()));
        batch.set(userFeed, feedResponse);
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    feedResponse.setTimeUpdated(new Date());
                    result.setValue(new ApiResponse<>(feedResponse, true, null));
                })
                .addOnFailureListener(e -> {
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                });
        return result;
    }


    public LiveData<ApiResponse<List<FeedResponse>>> getGlobalFeeds(long after, int limit) {
        MutableLiveData<ApiResponse<List<FeedResponse>>> result = new MutableLiveData<>();
        Date dateAfter = new Date(after);
        mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                .limit(limit).startAfter(dateAfter)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FeedResponse> feedResponses = new ArrayList<>(limit);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        feedResponses.add(doc.toObject(FeedResponse.class));
                    }
                    Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                    result.setValue(new ApiResponse<>(feedResponses, true, null));
                }).addOnFailureListener(e -> {
            Timber.e("onFailure: %s", e.getMessage());
            result.setValue(new ApiResponse<>(null, false, e.getMessage()));
        });

        return result;
    }

    @Override
    public LiveData<ApiResponse<List<FeedResponse>>> getGlobalFeeds(String afterFeedId, int limit) {
        MutableLiveData<ApiResponse<List<FeedResponse>>> result = new MutableLiveData<>();
        DocumentReference feedDoc = mDb.collection(GLOBAL_FEEDS).document(afterFeedId);
        feedDoc.get().addOnSuccessListener(documentSnapshot ->
                mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                        .limit(limit)
                        .startAfter(documentSnapshot)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<FeedResponse> feedResponses = new ArrayList<>(limit);
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                feedResponses.add(doc.toObject(FeedResponse.class));
                            }
                            Timber.i("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size());
                            result.setValue(new ApiResponse<>(feedResponses, true, null));
                        }).addOnFailureListener(e -> {
                    Timber.e("onFailure: %s", e.getMessage());
                    result.setValue(new ApiResponse<>(null, false, e.getMessage()));
                })).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));

        return result;
    }

    @Override
    public LiveData<ApiResponse<List<FeedResponse>>> getUserFeed(String userId, long after, int limit) {
        MutableLiveData<ApiResponse<List<FeedResponse>>> result = new MutableLiveData<>();
        mDb.collection(USERS).document(userId).collection(FEEDS)
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .startAfter(new Date(after))
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FeedResponse> feedResponses = new ArrayList<>(limit);
                    for (DocumentSnapshot doc :
                            queryDocumentSnapshots) {
                        Timber.i("FeedResponse: %s", doc.toString());
                        feedResponses.add(doc.toObject(FeedResponse.class));
                    }
                    result.setValue(new ApiResponse<>(feedResponses, true, null));
                }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())));
        return result;
    }

    @Override
    public LiveData<ApiResponse<List<FeedResponse>>> getUserFeed(String userId, String afterFeedId, int limit) {
        MutableLiveData<ApiResponse<List<FeedResponse>>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(afterFeedId).get()
                .addOnSuccessListener(documentSnapshot -> mDb.collection(USERS).document(userId).collection(FEEDS)
                        .orderBy("timeCreated", Query.Direction.DESCENDING)
                        .startAfter(documentSnapshot)
                        .limit(limit)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<FeedResponse> feedResponses = new ArrayList<>(limit);
                            for (DocumentSnapshot doc :
                                    queryDocumentSnapshots) {
                                feedResponses.add(doc.toObject(FeedResponse.class));
                            }
                            result.setValue(new ApiResponse<>(feedResponses, true, null));
                        }).addOnFailureListener(e -> result.setValue(new ApiResponse<>(null, false, e.getMessage())))).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                result.setValue(new ApiResponse<>(null, false, e.getMessage()));
            }
        });

        return result;
    }

    @Override
    public LiveData<ApiResponse<FeedResponse>> getFeed(String feedId) {
        MutableLiveData<ApiResponse<FeedResponse>> result = new MutableLiveData<>();
        mDb.collection(GLOBAL_FEEDS).document(feedId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    FeedResponse feedResponse = queryDocumentSnapshots.toObject(FeedResponse.class);
                    Timber.i("onSuccess: loaded %s feedResponse(s)", feedResponse);
                    result.setValue(new ApiResponse<>(feedResponse, true, null));
                }).addOnFailureListener(e -> {
            Timber.e("onFailure: %s", e.getMessage());
            result.setValue(new ApiResponse<>(null, false, e.getMessage()));
        });
        return result;
    }

    @Override
    public LiveData<ApiResponse<FeedResponse>> likeFeed(String feedId) {
        mDb.runTransaction(new Transaction.Function<FeedResponse>() {
            @Nullable
            @Override
            public FeedResponse apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {

                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<FeedResponse>() {
            @Override
            public void onSuccess(FeedResponse feedResponse) {

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

    @Override
    public void updateUser(UserEntity userEntity, SuccessCallback<Void> callback) {
        DocumentReference userDoc = mDb.collection(USERS).document();
        userEntity.setUserId(userDoc.getId());
        userDoc.set(userEntity, SetOptions.merge())
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
