package com.petnbu.petnbu.feed;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import java.util.ArrayList;

public class FeedsRepository implements FeedDataSource {

    private volatile static FeedsRepository INSTANCE = null;
    private MutableLiveData<ArrayList<Feed>> mFeeds;

    public static FeedsRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (FeedsRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FeedsRepository();
                }
            }
        }
        return INSTANCE;
    }

    private FeedsRepository() {
        mFeeds = new MutableLiveData<>();
        ArrayList<Feed> feeds = new ArrayList<>();
        Feed feed = new Feed();
        feed.setId(1);
        feed.setContent("SOLID Single Responsibility, Open Close, Liskov, SOLID Single Responsibility, Open Close, Liskov, kkk");
        feed.setUserName("Nhat Pham");
        feed.setUserAvatarUrl("https://academy-stg-assets.s3.amazonaws.com/user_50/hN5JRUzjs8nXifDEWnBc1522315546980_optimized.jpg");
        feed.setPhotoUrl("https://academy-stg-assets.s3.amazonaws.com/wall/user_50/32bTogWIn0R1xiSmZ1vM1523588835014.jpg");
        feed.setWidth(640);
        feed.setHeight(480);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(2);
        feed.setContent("Researcher");
        feed.setUserName("Sang Pham");
        feed.setUserAvatarUrl("https://picsum.photos/54/54/?random");

        ArrayList<Feed.Photo> photos = new ArrayList<>();
        Feed.Photo photo = new Feed.Photo();
        photo.setUrl("https://picsum.photos/760/900/?random");
        photo.setWidth(760);
        photo.setHeight(900);
        photos.add(photo);

        photo = new Feed.Photo();
        photo.setUrl("https://picsum.photos/540/760/?random");
        photo.setWidth(540);
        photo.setHeight(760);
        photos.add(photo);

        photo = new Feed.Photo();
        photo.setUrl("https://picsum.photos/620/1000/?random");
        photo.setWidth(620);
        photo.setHeight(1000);
        photos.add(photo);
        feed.setPhotos(photos);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(3);
        feed.setContent("Mingle LTD");
        feed.setUserName("Thanh Nguyen");
        feed.setUserAvatarUrl("https://picsum.photos/55/55/?random");
        feed.setPhotoUrl("https://picsum.photos/1268/652/?random");
        feed.setWidth(1268);
        feed.setHeight(652);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(4);
        feed.setContent("JSH");
        feed.setUserName("Ho Nguyen");
        feed.setUserAvatarUrl("https://picsum.photos/56/56/?random");
        feed.setPhotoUrl("https://picsum.photos/810/650/?random");
        feed.setWidth(812);
        feed.setHeight(650);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(5);
        feed.setContent("Mingle2");
        feed.setUserName("Hien Nguyen");
        feed.setUserAvatarUrl("https://picsum.photos/57/57/?random");

        photos = new ArrayList<>();
        photo = new Feed.Photo();
        photo.setUrl("https://picsum.photos/660/780/?random");
        photo.setWidth(660);
        photo.setHeight(780);
        photos.add(photo);

        photo = new Feed.Photo();
        photo.setUrl("https://picsum.photos/458/660/?random");
        photo.setWidth(458);
        photo.setHeight(660);
        photos.add(photo);

        photo = new Feed.Photo();
        photo.setUrl("https://picsum.photos/880/1120/?random");
        photo.setWidth(880);
        photo.setHeight(1120);
        photos.add(photo);
        feed.setPhotos(photos);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(6);
        feed.setContent("Academy");
        feed.setUserName("Nam Dinh");
        feed.setUserAvatarUrl("https://picsum.photos/58/58/?random");
        feed.setPhotoUrl("https://picsum.photos/640/1080/?random");
        feed.setWidth(640);
        feed.setHeight(1080);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(7);
        feed.setContent("Mingle2");
        feed.setUserName("Thuan Duc");
        feed.setUserAvatarUrl("https://picsum.photos/59/59/?random");
        feed.setPhotoUrl("https://picsum.photos/980/620/?random");
        feed.setWidth(980);
        feed.setHeight(620);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(8);
        feed.setContent("Mingle2");
        feed.setUserName("Than Banh");
        feed.setUserAvatarUrl("https://picsum.photos/60/60/?random");
        feed.setPhotoUrl("https://picsum.photos/480/480/?random");
        feed.setWidth(480);
        feed.setHeight(480);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(9);
        feed.setContent("Manager");
        feed.setUserName("Khiem Le");
        feed.setUserAvatarUrl("https://picsum.photos/61/61/?random");
        feed.setPhotoUrl("https://picsum.photos/468/720/?random");
        feed.setWidth(468);
        feed.setHeight(720);
        feeds.add(feed);

        feed = new Feed();
        feed.setId(10);
        feed.setContent("Leader");
        feed.setUserName("Duc Tran");
        feed.setUserAvatarUrl("https://picsum.photos/62/62/?random");
        feed.setPhotoUrl("https://picsum.photos/1080/612/?random");
        feed.setWidth(1080);
        feed.setHeight(612);
        feeds.add(feed);
        mFeeds.setValue(feeds);
    }

    @Override
    public LiveData<ArrayList<Feed>> getFeeds() {
        return mFeeds;
    }
}
