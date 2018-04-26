package com.petnbu.petnbu.di;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.feed.CreateFeedViewModel;
import com.petnbu.petnbu.feed.FeedsViewModel;
import com.petnbu.petnbu.login.LoginJavaActivity;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, WebServiceModule.class})
public interface AppComponent {

    void inject(LoginJavaActivity activity);

    void inject(FeedsViewModel viewModel);

    void inject(CreateFeedViewModel viewModel);

    PetDb getPetDb();

    FeedRepository getFeedRepo();

    UserRepository getUserRepo();

    AppExecutors getAppExecutor();
}
