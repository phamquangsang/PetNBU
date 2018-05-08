package com.petnbu.petnbu;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.FeedUIModel;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.repo.FeedRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FeedResponseApiTest {
    private static final String TAG = FeedResponseApiTest.class.getSimpleName();

    @Test
    public void testLoadFeed() {
        FeedUser userNhat = new FeedUser("2", "https://developer.android.com/static/images/android_logo_2x.png", "Nhat Nhat");
        List<Photo> photo2 = new ArrayList<>();
        photo2.add(new Photo("https://picsum.photos/1000/600/?image=383", "https://picsum.photos/500/300/?image=383", "https://picsum.photos/250/150/?image=383", "https://picsum.photos/100/60/?image=383", 1000, 600));
        FeedResponse feedResponse1 = new FeedResponse("2", userNhat, photo2, 12, 14, "", new Date(), new Date(), FeedEntity.STATUS_NEW);

        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        PetDb petDb = PetApplication.getAppComponent().getPetDb();
        appExecutors.diskIO().execute(() -> {
            petDb.feedDao().insertFromFeed(feedResponse1);

        });


    }

    @Test
    public void testFeedsCreateApi() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firestore);

        FeedUser userSang = new FeedUser("1", "https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/photo.jpg?sz=64",
                "Sang Sang");
        List<Photo> photo1 = new ArrayList<>();
        photo1.add(new Photo("https://picsum.photos/1200/1300/?image=381", "https://picsum.photos/600/650/?image=381", "https://picsum.photos/300/325/?image=381", "https://picsum.photos/120/130/?image=381", 1200, 1300));
        photo1.add(new Photo("https://picsum.photos/1200/1300/?image=382", "https://picsum.photos/600/650/?image=382", "https://picsum.photos/300/325/?image=382", "https://picsum.photos/120/130/?image=382", 1200, 1300));
        FeedResponse feedResponse = new FeedResponse("1", userSang, photo1, 10, 12, "", new Date(), new Date(), FeedEntity.STATUS_NEW);

        FeedUser userNhat = new FeedUser("2", "https://developer.android.com/static/images/android_logo_2x.png", "Nhat Nhat");
        List<Photo> photo2 = new ArrayList<>();
        photo2.add(new Photo("https://picsum.photos/1000/600/?image=383", "https://picsum.photos/500/300/?image=383", "https://picsum.photos/250/150/?image=383", "https://picsum.photos/100/60/?image=383", 1000, 600));
        FeedResponse feedResponse1 = new FeedResponse("2", userNhat, photo2, 12, 14, "", new Date(), new Date(),FeedEntity.STATUS_NEW);

        for (int i = 0; i < 30; i++) {
            webService.createFeed(i % 2 == 0 ? feedResponse : feedResponse1);
            try {
                Thread.sleep(100);
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
        LiveData<ApiResponse<List<FeedResponse>>> result = webService.getGlobalFeeds(date.getTime(), 30);
        result.observeForever(listApiResponse -> {
            assert listApiResponse != null;
            List<FeedResponse> feedResponses = listApiResponse.body;
            Log.i(TAG, "onChanged: feedResponses == null ? " + (feedResponses == null));
            signal.countDown();
            if (feedResponses != null) {
                assertThat(feedResponses.size(), is(30));
                for (int i = 0; i < feedResponses.size() - 1; i++) {
                    assertThat(feedResponses.get(i).getTimeCreated().before(new Date()), is(true));
                    assertThat(feedResponses.get(i + 1).getTimeCreated().getTime() <= feedResponses.get(i).getTimeCreated().getTime(), is(true));
                }
                signal.countDown();
            }
        });
        signal.await(30, TimeUnit.SECONDS);

        FeedRepository repository = PetApplication.getAppComponent().getFeedRepo();

        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                LiveData<Resource<List<FeedUIModel>>> resultLive = repository.loadFeeds(FeedPaging.GLOBAL_FEEDS_PAGING_ID);
                resultLive.observeForever(new Observer<Resource<List<FeedUIModel>>>() {
                    @Override
                    public void onChanged(@Nullable Resource<List<FeedUIModel>> listResource) {
                        if (listResource != null) {
                            Timber.i(listResource.toString());
                            if (listResource.data != null) {
                                Timber.i(String.valueOf(listResource.data.size()));
                            }
                        }
                    }
                });
            }
        });

        signal.await(20, TimeUnit.SECONDS);
    }


    @Test
    public void migrateData() throws InterruptedException {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        final CountDownLatch signal = new CountDownLatch(2);
        WebService webService = new FirebaseService(firestore);

        webService.getGlobalFeeds(System.currentTimeMillis(), 200).observeForever(new Observer<ApiResponse<List<FeedResponse>>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<List<FeedResponse>> listApiResponse) {
                WriteBatch batch = firestore.batch();
                if (listApiResponse != null && listApiResponse.isSucceed) {
                    List<FeedResponse> list = listApiResponse.body;
                    for (FeedResponse item : list) {
                        String path = String.format("user_feeds/%s/feeds/%s", item.getFeedUser().getUserId(), item.getFeedId());
                        DocumentReference ref = firestore.document(path);
                        batch.set(ref, item);
                    }
                }
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timber.i("succeed");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.e(e);
                    }
                });
            }
        });

        signal.await(20, TimeUnit.SECONDS);
    }
}
