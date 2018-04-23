package com.petnbu.petnbu;

import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.SuccessCallback;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FeedApiTest {
    private static final String TAG = FeedApiTest.class.getSimpleName();

    @Test
    public void testFeedsCreateApi() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firestore);

        FeedUser userSang = new FeedUser("1", "https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/photo.jpg?sz=64",
                "Sang Sang");
        List<Photo> photo1 = new ArrayList<>();
        photo1.add(new Photo("https://picsum.photos/1200/1300/?image=381", "https://picsum.photos/600/650/?image=381", "https://picsum.photos/300/325/?image=381", "https://picsum.photos/120/130/?image=381", 1200, 1300));
        photo1.add(new Photo("https://picsum.photos/1200/1300/?image=382", "https://picsum.photos/600/650/?image=382", "https://picsum.photos/300/325/?image=382", "https://picsum.photos/120/130/?image=382", 1200, 1300));
        Feed feed = new Feed("1", userSang, photo1, 10, 12, new Date(), new Date());

        FeedUser userNhat = new FeedUser("2", "https://developer.android.com/static/images/android_logo_2x.png", "Nhat Nhat");
        List<Photo> photo2 = new ArrayList<>();
        photo2.add(new Photo("https://picsum.photos/1000/600/?image=383", "https://picsum.photos/500/300/?image=383", "https://picsum.photos/250/150/?image=383", "https://picsum.photos/100/60/?image=383", 1000, 600));
        Feed feed1 = new Feed("2", userNhat, photo2, 12, 14, new Date(), new Date());

        for (int i = 0; i < 100; i++) {
            webService.createFeed(i % 2 == 0 ? feed : feed1, new SuccessCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "onSuccess: create feed success");
                }

                @Override
                public void onFailed(Exception e) {
                    Log.e(TAG, "onFailed: ", e);
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    @Test
    public void testFeedsPagingApi() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(2);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firestore);
        Date date = new Date();
        LiveData<List<Feed>> result = webService.getFeeds(date.getTime(), 30);
        result.observeForever(new Observer<List<Feed>>() {
            @Override
            public void onChanged(@Nullable List<Feed> feeds) {
                Log.i(TAG, "onChanged: feeds == null ? " + (feeds == null));
                signal.countDown();
                if (feeds != null) {
                    assertThat(feeds.size(), is(30));
                    for (int i = 0; i < feeds.size() - 1 ; i++) {
                        assertThat(feeds.get(i).getTimeCreated().before(new Date()), is(true));
                        assertThat(feeds.get(i + 1).getTimeCreated().getTime() <= feeds.get(i).getTimeCreated().getTime(), is(true));
                    }
                    signal.countDown();
                }
            }
        });
        signal.await(30, TimeUnit.SECONDS);
    }

}
