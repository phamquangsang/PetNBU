package com.petnbu.petnbu;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.SuccessCallback;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.User;
import com.petnbu.petnbu.repo.NetworkBoundResource;
import com.petnbu.petnbu.repo.UserRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import timber.log.Timber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class UserApiTest {
    @Test
    public void testGetUserById(){
        Photo photo = (new Photo("https://picsum.photos/1200/1300/?image=381", "https://picsum.photos/600/650/?image=381", "https://picsum.photos/300/325/?image=381", "https://picsum.photos/120/130/?image=381", 1200, 1300));
        User user = new User("1234", photo, "Sang Sang", "phamsang@gmail.com", new Date(), new Date());

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firebaseFirestore);

        CountDownLatch signal = new CountDownLatch(1);
        webService.createUser(user, new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                signal.countDown();

            }

            @Override
            public void onFailed(Exception e) {

            }
        });

        UserRepository userRepository = PetApplication.getAppComponent().getUserRepo();
        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        CountDownLatch signal1 = new CountDownLatch(3);
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                userRepository.getUserById("1234").observeForever(new Observer<Resource<User>>() {
                    @Override
                    public void onChanged(@Nullable Resource<User> userResource) {
                        signal1.countDown();
                        if(userResource !=null && userResource.data!= null){
                            Timber.i(userRepository.toString());
                            User result = userResource.data;
                            assertThat(result.getUserId(), is("1234"));
                            assertThat(result.getName(), is("Sang Sang"));
                            assertThat(result.getEmail(), is("phamsang@gmail.com"));
                        }
                    }
                });
            }
        });

    }
}
