package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FakeWebService implements WebService {

    @Override
    public LiveData<ApiResponse<Feed>> createFeed(Feed feed) {
        return null;
    }


    @Override
    public LiveData<ApiResponse<User>> createUser(User user) {
        return null;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(long after, int limit) {
        MutableLiveData<ApiResponse<List<Feed>>> mFeeds = new MutableLiveData<>();
        List<Feed> feeds = new ArrayList<>();

        // 1
        Feed feed = new Feed();
        feed.setFeedId("1");
        feed.setContent("SOLID Single Responsibility, Open Close, Liskov, SOLID Single Responsibility, Open Close, Liskov, kkk");

        FeedUser feedUser = new FeedUser();
        feedUser.setUserId("1");
        feedUser.setDisplayName("Nhat Pham");
        feedUser.setPhotoUrl("https://academy-stg-assets.s3.amazonaws.com/user_50/hN5JRUzjs8nXifDEWnBc1522315546980_optimized.jpg");
        feed.setFeedUser(feedUser);

        List<Photo> photos = new ArrayList<>();
        photos.add(new Photo("https://academy-stg-assets.s3.amazonaws.com/wall/user_50/32bTogWIn0R1xiSmZ1vM1523588835014.jpg",
                "", "", "", 640, 480));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 2
        feed = new Feed();
        feed.setFeedId("2");
        feed.setContent("Researcher");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Nhat Pham");
        feedUser.setPhotoUrl("https://picsum.photos/54/54/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/760/900/?random",
                "", "", "", 760, 900));
        photos.add(new Photo("https://picsum.photos/540/760/?random",
                "", "", "", 540, 760));
        photos.add(new Photo("https://picsum.photos/620/1000/?random",
                "", "", "", 620, 1000));
        feed.setPhotos(photos);
        feeds.add(feed);


        // 3
        feed = new Feed();
        feed.setFeedId("3");
        feed.setContent("Mingle LTD");

        feedUser = new FeedUser();
        feedUser.setUserId("3");
        feedUser.setDisplayName("Thanh Nguyen");
        feedUser.setPhotoUrl("https://picsum.photos/55/55/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/1268/652/?random",
                "", "", "", 1268, 652));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 4
        feed = new Feed();
        feed.setFeedId("4");
        feed.setContent("JSH");

        feedUser = new FeedUser();
        feedUser.setUserId("4");
        feedUser.setDisplayName("Ho Nguyen");
        feedUser.setPhotoUrl("https://picsum.photos/56/56/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/810/650/?random",
                "", "", "", 810, 650));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 5
        feed = new Feed();
        feed.setFeedId("5");
        feed.setContent("Mingle2");

        feedUser = new FeedUser();
        feedUser.setUserId("5");
        feedUser.setDisplayName("Hien Nguyen");
        feedUser.setPhotoUrl("https://picsum.photos/57/57/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/660/780/?random",
                "", "", "", 660, 780));
        photos.add(new Photo("https://picsum.photos/458/660/?random",
                "", "", "", 458, 660));
        photos.add(new Photo("https://picsum.photos/880/1120/?random",
                "", "", "", 880, 1120));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 6
        feed = new Feed();
        feed.setFeedId("6");
        feed.setContent("Academy");

        feedUser = new FeedUser();
        feedUser.setUserId("6");
        feedUser.setDisplayName("Nam Dinh");
        feedUser.setPhotoUrl("https://picsum.photos/58/58/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/640/1080/?random",
                "", "", "", 640, 1080));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 7
        feed = new Feed();
        feed.setFeedId("7");
        feed.setContent("Mingle2");

        feedUser = new FeedUser();
        feedUser.setUserId("7");
        feedUser.setDisplayName("Thuan Duc");
        feedUser.setPhotoUrl("https://picsum.photos/59/59/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/980/620/?random",
                "", "", "", 980, 620));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 8
        feed = new Feed();
        feed.setFeedId("8");
        feed.setContent("Mingle2");

        feedUser = new FeedUser();
        feedUser.setUserId("8");
        feedUser.setDisplayName("Than Banh");
        feedUser.setPhotoUrl("https://picsum.photos/60/60/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/480/480/?random",
                "", "", "", 480, 480));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 9
        feed = new Feed();
        feed.setFeedId("9");
        feed.setContent("Manager");

        feedUser = new FeedUser();
        feedUser.setUserId("9");
        feedUser.setDisplayName("Khiem Le");
        feedUser.setPhotoUrl("https://picsum.photos/61/61/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/468/720/?random",
                "", "", "", 468, 720));
        feed.setPhotos(photos);
        feeds.add(feed);

        // 10
        feed = new Feed();
        feed.setFeedId("10");
        feed.setContent("Leader");

        feedUser = new FeedUser();
        feedUser.setUserId("10");
        feedUser.setDisplayName("Duc Tran");
        feedUser.setPhotoUrl("https://picsum.photos/62/62/?random");
        feed.setFeedUser(feedUser);

        photos = new ArrayList<>();
        photos.add(new Photo("https://picsum.photos/1080/612/?random",
                "", "", "", 1080, 612));
        feed.setPhotos(photos);
        feeds.add(feed);
        mFeeds.setValue(new ApiResponse<>(feeds, true, null));

        return mFeeds;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(String afterFeedId, int limit) {
        return null;
    }

    @Override
    public void updateUser(User user, SuccessCallback<Void> callback) {

    }

    @Override
    public LiveData<ApiResponse<User>> getUser(String userId) {
        return new MutableLiveData<>();
    }

    @Override
    public LiveData<ApiResponse<Feed>> likeFeed(String feedId) {
        return null;
    }

    @Override
    public LiveData<ApiResponse<Feed>> getFeed(String feedId) {
        return null;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, long after, int limit) {
        return null;
    }

    @Override
    public LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, String afterFeedId, int limit) {
        return null;
    }
    public LiveData<ApiResponse<List<Comment>>> getComments(String feedId) {
        MutableLiveData<ApiResponse<List<Comment>>> commentsLiveData = new MutableLiveData<>();
        List<Comment> comments = new ArrayList<>();

        Comment comment = new Comment();
        comment.setId("1");
        comment.setContent("Well long live all those lies!!\n" +
                "You tell yourself\n" +
                "You'll be alright\n" +
                "But there's no kiss goodbye\n" +
                "And only the end\n" +
                "And only the night");

        FeedUser feedUser = new FeedUser();
        feedUser.setUserId("1");
        feedUser.setDisplayName("Nhat Pham");
        feedUser.setPhotoUrl("https://lh4.googleusercontent.com/-pBi1Gc4MU6Y/AAAAAAAAAAI/AAAAAAAAA18/A7gZ4lFOrl8/s96-c/photo.jpg");
        comment.setFeedUser(feedUser);
        comment.setLikeCount(1);
        comment.setCommentCount(5);
        comment.setTimeCreated(new Date());

        Comment latestComment = new Comment();
        latestComment.setId("4");
        latestComment.setContent("What is this What is this What is this What is this");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Sang Pham");
        feedUser.setPhotoUrl("https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/s96-c/photo.jpg");
        latestComment.setFeedUser(feedUser);
        comment.setLatestComment(latestComment);

        comments.add(comment);

        comment = new Comment();
        comment.setId("2");
        comment.setContent("What is this place");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Sang Pham");
        feedUser.setPhotoUrl("https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/s96-c/photo.jpg");
        comment.setFeedUser(feedUser);
        comment.setLikeCount(0);
        comment.setCommentCount(1);
        comment.setTimeCreated(new Date());
        comments.add(comment);

        comment = new Comment();
        comment.setId("3");
        comment.setContent("What is this thing");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Sang Pham");
        feedUser.setPhotoUrl("https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/s96-c/photo.jpg");
        comment.setFeedUser(feedUser);
        comment.setLikeCount(5);
        comment.setCommentCount(0);
        comment.setTimeCreated(new Date());
        comments.add(comment);

        commentsLiveData.setValue(new ApiResponse<>(comments, true, null));
        return commentsLiveData;
    }

    @Override
    public LiveData<ApiResponse<List<Comment>>> getCommentsByComment(String commentId) {
        MutableLiveData<ApiResponse<List<Comment>>> commentsLiveData = new MutableLiveData<>();
        List<Comment> comments = new ArrayList<>();

        Comment comment = new Comment();
        comment.setId("1");
        comment.setContent("Well long live all those lies!!\n" +
                "You tell yourself\n" +
                "You'll be alright\n" +
                "But there's no kiss goodbye\n" +
                "And only the end\n" +
                "And only the night");

        FeedUser feedUser = new FeedUser();
        feedUser.setUserId("1");
        feedUser.setDisplayName("Nhat Pham");
        feedUser.setPhotoUrl("https://lh4.googleusercontent.com/-pBi1Gc4MU6Y/AAAAAAAAAAI/AAAAAAAAA18/A7gZ4lFOrl8/s96-c/photo.jpg");
        comment.setFeedUser(feedUser);
        comment.setLikeCount(1);
        comment.setCommentCount(5);
        comment.setTimeCreated(new Date());

        Comment latestComment = new Comment();
        latestComment.setId("4");
        latestComment.setContent("What is this What is this What is this What is this");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Sang Pham");
        feedUser.setPhotoUrl("https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/s96-c/photo.jpg");
        latestComment.setFeedUser(feedUser);
        comment.setLatestComment(latestComment);

        comments.add(comment);

        comment = new Comment();
        comment.setId("2");
        comment.setContent("What is this place");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Sang Pham");
        feedUser.setPhotoUrl("https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/s96-c/photo.jpg");
        comment.setFeedUser(feedUser);
        comment.setLikeCount(0);
        comment.setCommentCount(1);
        comment.setTimeCreated(new Date());
        comments.add(comment);

        comment = new Comment();
        comment.setId("3");
        comment.setContent("What is this thing");

        feedUser = new FeedUser();
        feedUser.setUserId("2");
        feedUser.setDisplayName("Sang Pham");
        feedUser.setPhotoUrl("https://lh5.googleusercontent.com/-FJzPGWw8bAk/AAAAAAAAAAI/AAAAAAAAAu8/GohAJXC8_78/s96-c/photo.jpg");
        comment.setFeedUser(feedUser);
        comment.setLikeCount(5);
        comment.setCommentCount(0);
        comment.setTimeCreated(new Date());
        comments.add(comment);

        commentsLiveData.setValue(new ApiResponse<>(comments, true, null));
        return commentsLiveData;
    }
}
