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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirebaseService implements WebService{
    
    public static final String GLOBAL_FEEDS = "global_feeds";

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
            if(documentSnapshot.exists()){
                callback.onSuccess(documentSnapshot.toObject(Feed.class));
            }else{
                callback.onFailed(new IllegalStateException(String.format("Feed Id: %s does not exist", feedId)));
            }
        }).addOnFailureListener(e -> callback.onFailed(e));
    }

    public LiveData<List<Feed>> getFeeds(long after, int limit){
        MutableLiveData<List<Feed>> feedsLive = new MutableLiveData<>();
        List<Feed> feeds = new ArrayList<>(limit);
        Date dateAfter = new Date(after);
        mDb.collection(GLOBAL_FEEDS).orderBy("timeCreated", Query.Direction.DESCENDING)
                .limit(limit).startAfter(dateAfter)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc :
                            queryDocumentSnapshots.getDocuments()) {
                        feeds.add(doc.toObject(Feed.class));
                    }
                    Log.i(TAG, String.format("onSuccess: loaded %d feed(s)", queryDocumentSnapshots.getDocuments().size()));
                    feedsLive.setValue(feeds);
                }).addOnFailureListener(e -> Log.e(TAG, "onFailure: ", e));
        return feedsLive;
    }
}
