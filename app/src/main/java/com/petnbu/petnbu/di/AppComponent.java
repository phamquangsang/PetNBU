package com.petnbu.petnbu.di;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.feed.CreateEditFeedViewModel;
import com.petnbu.petnbu.feed.FeedsViewModel;
import com.petnbu.petnbu.feed.comment.CommentsViewModel;
import com.petnbu.petnbu.jobs.CreateEditFeedWorker;
import com.petnbu.petnbu.login.LoginJavaActivity;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;
import com.petnbu.petnbu.userprofile.UserProfileViewModel;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, WebServiceModule.class})
public interface AppComponent {

    void inject(LoginJavaActivity activity);

    void inject(FeedsViewModel viewModel);

    void inject(CreateEditFeedWorker createEditFeedWorker);

    void inject(CreateEditFeedViewModel viewModel);

    void inject(UserProfileViewModel userProfileViewModel);

    void inject(CommentsViewModel commentsViewModel);

    PetDb getPetDb();

    FeedRepository getFeedRepo();

    UserRepository getUserRepo();

    AppExecutors getAppExecutor();

    FirebaseJobDispatcher getJobDispatcher();


}
