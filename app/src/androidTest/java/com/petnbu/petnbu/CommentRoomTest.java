package com.petnbu.petnbu;

import android.support.test.runner.AndroidJUnit4;

import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentEntity;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedUI;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

@RunWith(AndroidJUnit4.class)
public class CommentRoomTest {

    @Test
    public void testQueryFeedUI(){
        PetDb petDb = PetApplication.getAppComponent().getPetDb();
        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        CountDownLatch signal =new CountDownLatch(1);
        appExecutors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                CommentEntity comment = petDb.commentDao().getCommentById("bFD4manPwZG5Udzdb5g0");
                FeedUI feedUI =
                        petDb.feedDao().getFeedUI("01AHzDY3dxhCP74ZpbWy");
                FeedEntity feed = petDb.feedDao().findFeedEntityById("01AHzDY3dxhCP74ZpbWy");
                Timber.i("feedUI: %s", feedUI);
                signal.countDown();
            }
        });
        try {
            signal.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
