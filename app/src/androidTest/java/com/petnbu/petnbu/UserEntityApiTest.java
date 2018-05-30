package com.petnbu.petnbu;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.FirebaseService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.repo.UserRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import timber.log.Timber;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class UserEntityApiTest {
    @Test
    public void testGetUserById(){
        Photo photo = (new Photo("https://picsum.photos/1200/1300/?image=381", "https://picsum.photos/600/650/?image=381", "https://picsum.photos/300/325/?image=381", "https://picsum.photos/120/130/?image=381", 1200, 1300));
        UserEntity userEntity = new UserEntity("1234", photo, "Sang Sang", "phamsang@gmail.com", new Date(), new Date());

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        WebService webService = new FirebaseService(firebaseFirestore, PetApplication.getAppComponent().getAppExecutor());

        CountDownLatch signal = new CountDownLatch(1);
        LiveData<ApiResponse<UserEntity>> apiResponse = webService.createUser(userEntity);
        apiResponse.observeForever(new Observer<ApiResponse<UserEntity>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<UserEntity> userApiResponse) {
                if(userApiResponse != null){
                    if(userApiResponse.isSucceed && userApiResponse.body != null){
                        signal.countDown();
                    }
                    apiResponse.removeObserver(this);
                }
            }
        });

        UserRepository userRepository = PetApplication.getAppComponent().getUserRepo();
        AppExecutors appExecutors = PetApplication.getAppComponent().getAppExecutor();
        CountDownLatch signal1 = new CountDownLatch(3);
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                userRepository.getUserById("1234").observeForever(new Observer<Resource<UserEntity>>() {
                    @Override
                    public void onChanged(@Nullable Resource<UserEntity> userResource) {
                        signal1.countDown();
                        if(userResource !=null && userResource.data!= null){
                            Timber.i(userRepository.toString());
                            UserEntity result = userResource.data;
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
