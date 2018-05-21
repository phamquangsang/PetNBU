package com.petnbu.petnbu;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.util.IdUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class CommentApiTest {

    @Test
    public void createComment() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firestore);

        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        CountDownLatch signalFeed = new CountDownLatch(1);

        appExecutors.networkIO().execute(() ->{
            LiveData<ApiResponse<Feed>> feedApiResponse = webService.getFeed("01AHzDY3dxhCP74ZpbWy");
            feedApiResponse.observeForever(new Observer<ApiResponse<Feed>>() {
                @Override
                public void onChanged(@Nullable ApiResponse<Feed> feedResponseApiResponse) {
                    if(feedResponseApiResponse!= null && feedResponseApiResponse.isSucceed){
                        feedApiResponse.removeObserver(this);
                        Feed feed = feedResponseApiResponse.body;
                        Comment comment = new Comment();
                        comment.setParentFeedId(feed.getFeedId());
                        comment.setContent("this is the test comment");
                        comment.setFeedUser(feed.getFeedUser());
                        comment.setId(IdUtil.generateID("comment"));
                        LiveData<ApiResponse<Comment>> createCommentResult =
                                webService.createFeedComment(comment, feed.getFeedId());

                        createCommentResult.observeForever(new Observer<ApiResponse<Comment>>() {
                            @Override
                            public void onChanged(@Nullable ApiResponse<Comment> commentApiResponse) {
                                if(commentApiResponse != null){
                                    if(commentApiResponse.isSucceed){
                                        createCommentResult.removeObserver(this);
                                        Comment commentCreated = commentApiResponse.body;
                                        Timber.i("comment create succeed: %s", commentCreated);
                                        assertThat(commentCreated.getId().equals(comment.getId()), is(true));
                                        signalFeed.countDown();
                                    }else{
                                        createCommentResult.removeObserver(this);
                                        Timber.e("create Comment Error: %s", commentApiResponse.errorMessage);
                                        signalFeed.countDown();
                                    }
                                }
                            }
                        });
                    }else if(feedResponseApiResponse != null){
                        Timber.e("Error %s", feedResponseApiResponse.errorMessage);
                    }
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
    public void createReply(){
        String parentCommentId = "9OcPdLhgmWRphFQxAfCb";
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firestore);

        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        PetDb petDb = PetApplication.getAppComponent().getPetDb();
        CountDownLatch signalFeed = new CountDownLatch(1);

        appExecutors.networkIO().execute(() ->{

            UserEntity owner = petDb.userDao().findUserById(SharedPrefUtil.getUserId());

            Comment comment = new Comment();
            comment.setParentCommentId(parentCommentId);
            comment.setContent("this is the test reply comment");
            comment.setFeedUser(new FeedUser(owner.getUserId(), owner.getAvatar(), owner.getName()));
            comment.setId(IdUtil.generateID("subComment"));
            LiveData<ApiResponse<Comment>> createCommentResult =
                    webService.createReplyComment(comment, parentCommentId);

            createCommentResult.observeForever(new Observer<ApiResponse<Comment>>() {
                @Override
                public void onChanged(@Nullable ApiResponse<Comment> commentApiResponse) {
                    if(commentApiResponse != null){
                        if(commentApiResponse.isSucceed){
                            createCommentResult.removeObserver(this);
                            Comment commentCreated = commentApiResponse.body;
                            Timber.i("comment create succeed: %s", commentCreated);
                            assertThat(commentCreated.getId().equals(comment.getId()), is(true));
                            signalFeed.countDown();
                        }else{
                            createCommentResult.removeObserver(this);
                            Timber.e("create Comment Error: %s", commentApiResponse.errorMessage);
                            signalFeed.countDown();
                        }
                    }
                }
            });
        });

        try {
            signalFeed.await(5000, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
