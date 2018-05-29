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
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.repo.FeedRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static com.petnbu.petnbu.api.FirebaseService.GLOBAL_FEEDS;
import static com.petnbu.petnbu.model.LocalStatus.STATUS_NEW;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class FeedApiTest {
    private static final String TAG = FeedApiTest.class.getSimpleName();
    private Map<String, FeedUser> feedUsers = new HashMap<>();

    @Test
    public void migrateData() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        FirebaseService webService = new FirebaseService(firestore, appExecutors);
        
        CountDownLatch countDownLatch = new CountDownLatch(1);
        
        webService.getAllUser().observeForever(listApiResponse -> {
            if(listApiResponse != null){
                if(listApiResponse.isSucceed){
                    List<UserEntity> users = listApiResponse.body;
                    feedUsers = translateToFeedUsers(users);
                    webService.getGlobalFeeds(new Date().getTime(), 200).observeForever(listApiResponse1 -> {
                        if(listApiResponse1 != null){
                            if(listApiResponse1.isSucceed && listApiResponse1.body != null){
                                List<Feed> feeds = listApiResponse1.body;
                                WriteBatch batch = firestore.batch();

                                for (Feed feed : feeds) {
                                    feed.setFeedUser(feedUsers.get(feed.getFeedUser().getUserId()));
                                    DocumentReference doc = firestore.collection(GLOBAL_FEEDS).document(feed.getFeedId());
                                    batch.set(doc, feed);
                                    DocumentReference userFeed =
                                            firestore.document(String.format("users/%s/feeds/%s", feed.getFeedUser().getUserId(), feed.getFeedId()));
                                    batch.set(userFeed, feed);
                                }
                                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Timber.i("succeed");
                                        countDownLatch.countDown();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Timber.e(e);
                                    }
                                });

                            }
                        }
                    });
                }
            }
        });

        
        try {
            countDownLatch.await(20, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Map<String, FeedUser> translateToFeedUsers(List<UserEntity> users) {
        for (UserEntity user :
                users) {
            feedUsers.put(user.getUserId(), new FeedUser(user.getUserId(), user.getAvatar(), user.getName()));
        }
        return feedUsers;
    }


    @Test
    public void testLoadFeed() {
        Photo avatar = new Photo("https://developer.android.com/static/images/android_logo_2x.png", null, null, null, 0, 0);
        FeedUser userNhat = new FeedUser("2", avatar, "Nhat Nhat");
        List<Photo> photo2 = new ArrayList<>();
        photo2.add(new Photo("https://picsum.photos/1000/600/?image=383", "https://picsum.photos/500/300/?image=383", "https://picsum.photos/250/150/?image=383", "https://picsum.photos/100/60/?image=383", 1000, 600));
        Feed feed1 = new Feed("2", userNhat, photo2, 12, null, 14, false ,false, "", new Date(), new Date(), STATUS_NEW);

        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        PetDb petDb = PetApplication.getAppComponent().getPetDb();
        appExecutors.diskIO().execute(() -> {
            petDb.feedDao().insertFromFeed(feed1);

        });


    }

    @Test
    public void testFeedsCreateApi() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firestore, PetApplication.getAppComponent().getAppExecutor());

        Photo avatar = new Photo("\"https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/photo.jpg?sz=64\"", null, null, null, 0, 0);
        FeedUser userSang = new FeedUser("1", avatar,
                "Sang Sang");
        List<Photo> photo1 = new ArrayList<>();
        photo1.add(new Photo("https://picsum.photos/1200/1300/?image=381", "https://picsum.photos/600/650/?image=381", "https://picsum.photos/300/325/?image=381", "https://picsum.photos/120/130/?image=381", 1200, 1300));
        photo1.add(new Photo("https://picsum.photos/1200/1300/?image=382", "https://picsum.photos/600/650/?image=382", "https://picsum.photos/300/325/?image=382", "https://picsum.photos/120/130/?image=382", 1200, 1300));
        Feed feed = new Feed("1", userSang, photo1, 10, null, 12, false, false, "", new Date(), new Date(), STATUS_NEW);

        Photo avatarNhat = new Photo("https://developer.android.com/static/images/android_logo_2x.png", null, null, null, 0, 0);
        FeedUser userNhat = new FeedUser("2", avatarNhat, "Nhat Nhat");
        List<Photo> photo2 = new ArrayList<>();
        photo2.add(new Photo("https://picsum.photos/1000/600/?image=383", "https://picsum.photos/500/300/?image=383", "https://picsum.photos/250/150/?image=383", "https://picsum.photos/100/60/?image=383", 1000, 600));
        Feed feed1 = new Feed("2", userNhat, photo2, 12, null, 14, false, false,"", new Date(), new Date(), STATUS_NEW);

        for (int i = 0; i < 30; i++) {
            webService.createFeed(i % 2 == 0 ? feed : feed1);
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

        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        WebService webService = new FirebaseService(firestore ,appExecutors);
        Date date = new Date();
        LiveData<ApiResponse<List<Feed>>> result = webService.getGlobalFeeds(date.getTime(), 30);
        result.observeForever(listApiResponse -> {
            assert listApiResponse != null;
            List<Feed> feedRespons = listApiResponse.body;
            Log.i(TAG, "onChanged: feedRespons == null ? " + (feedRespons == null));
            signal.countDown();
            if (feedRespons != null) {
                assertThat(feedRespons.size(), is(30));
                for (int i = 0; i < feedRespons.size() - 1; i++) {
                    assertThat(feedRespons.get(i).getTimeCreated().before(new Date()), is(true));
                    assertThat(feedRespons.get(i + 1).getTimeCreated().getTime() <= feedRespons.get(i).getTimeCreated().getTime(), is(true));
                }
                signal.countDown();
            }
        });
        signal.await(30, TimeUnit.SECONDS);

        FeedRepository repository = PetApplication.getAppComponent().getFeedRepo();

        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                LiveData<Resource<List<FeedUI>>> resultLive = repository.loadFeeds(Paging.GLOBAL_FEEDS_PAGING_ID, "701Gx5cLL9W7H3RmLz4ZxEdFlpb2");
                resultLive.observeForever(new Observer<Resource<List<FeedUI>>>() {
                    @Override
                    public void onChanged(@Nullable Resource<List<FeedUI>> listResource) {
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


}
