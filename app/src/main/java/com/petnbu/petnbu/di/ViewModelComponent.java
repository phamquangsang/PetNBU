package com.petnbu.petnbu.di;

import com.petnbu.petnbu.feed.FeedsViewModel;

import javax.inject.Singleton;

import dagger.Component;
import dagger.Subcomponent;

public interface ViewModelComponent {

    void inject(FeedsViewModel viewModel);

}
