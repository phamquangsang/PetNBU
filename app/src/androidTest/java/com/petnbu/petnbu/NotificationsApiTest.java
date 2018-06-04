package com.petnbu.petnbu;

import android.arch.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Notification;

import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotificationsApiTest {
    @Test
    public void testLoadNotifications() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        WebService webService = new FirebaseService(firestore, appExecutors);
        CountDownLatch signalFeed = new CountDownLatch(1);

        appExecutors.networkIO().execute(() ->{
            LiveData<ApiResponse<List<Notification>>> feedApiResponse = webService.getNotifications("701Gx5cLL9W7H3RmLz4ZxEdFlpb2", new Date().getTime(), 10);
            feedApiResponse.observeForever(listApiResponse -> {
                assert listApiResponse != null;
                if(listApiResponse.isSuccessful()){
                    assert listApiResponse.getBody() != null;
                    assertThat(listApiResponse.getBody().size() == 0, is(false));
                    signalFeed.countDown();
                }
            });
        });

        try {
            signalFeed.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testNotiRepo(){

    }
}
