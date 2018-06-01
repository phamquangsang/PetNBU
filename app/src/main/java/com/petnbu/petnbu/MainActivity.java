package com.petnbu.petnbu;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.petnbu.petnbu.databinding.ActivityMainBinding;
import com.petnbu.petnbu.feed.FeedsFragment;
import com.petnbu.petnbu.login.LoginJavaActivity;
import com.petnbu.petnbu.notification.NotificationsFragment;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    private NavController mNavController;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = item -> {
        NavOptions navOptions = new NavOptions.Builder().setClearTask(true).build();
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        mNavController.navigate(R.id.feeds_fragment, null, navOptions);
                        return true;
                    case R.id.navigation_notifications:
                        mNavController.navigate(R.id.notification_fragment, null, navOptions);
                        return true;
                    case R.id.navigation_fragment3:
                        show3();
                        return true;
                }
                return false;
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mNavController = NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.navHostFragment));
        NavigationUI.setupWithNavController(mBinding.bottomNavigation, mNavController);
        getWindow().setBackgroundDrawable(null);

        if(isAuthenticationRequired()) {
            startActivity(new Intent(this, LoginJavaActivity.class));
            finish();
        } else {
            initialize();
        }
    }

    private boolean isAuthenticationRequired() {
        return FirebaseAuth.getInstance().getCurrentUser() == null;
    }

    private void initialize() {
        mBinding.bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mBinding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
    }

    private void showFeeds() {
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        Fragment feedFragment = fragmentManager.findFragmentByTag(FeedsFragment.class.getSimpleName());
//        if(feedFragment != null) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .hide(fragmentManager.findFragmentById(R.id.fragmentContainer))
//                    .show(feedFragment)
//                    .addToBackStack(FeedsFragment.class.getSimpleName())
//                    .commit();
//        } else {
//            feedFragment = new FeedsFragment();
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .add(R.id.fragmentContainer, feedFragment, FeedsFragment.class.getSimpleName())
//                    .commit();
//        }
    }

    private void showNotifications() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(NotificationsFragment.class.getSimpleName());
        if(fragment == null) {
            fragment = new NotificationsFragment();
        }
        showFragment(fragment);
    }

    private void show3() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(Fragment3.class.getSimpleName());
        if(fragment == null) {
            fragment = new Fragment3();
        }
        showFragment(fragment);
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String tagFragmentToAdd = fragment.getClass().getSimpleName();

        if(fragmentManager.findFragmentByTag(tagFragmentToAdd) != null) {
            ArrayMap<String, Fragment> mFragments = new ArrayMap<>();
            boolean isFound = false;
            for (int i = fragmentManager.getBackStackEntryCount() - 1; i >= 0; i--) {
                FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(i);
                if (backStackEntry.getName().equals(tagFragmentToAdd)) {
                    isFound = true;
                    break;
                } else {
                    mFragments.put(backStackEntry.getName(), fragmentManager.findFragmentByTag(backStackEntry.getName()));
                }
            }

            if(isFound) {
                mFragments.put(tagFragmentToAdd, fragment);
                if(FeedsFragment.class.getSimpleName().equals(mFragments.keyAt(0))) {
                    mFragments.removeAt(0);
                }
                fragmentManager.popBackStackImmediate(tagFragmentToAdd, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                Fragment previous = fragmentManager.findFragmentById(R.id.fragmentContainer);
                for (String tagBackStack : mFragments.keySet()) {
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.hide(previous);
                    Fragment fragmentToAdd = mFragments.get(tagBackStack);
                    fragmentTransaction.add(R.id.fragmentContainer, fragmentToAdd, tagBackStack)
                            .addToBackStack(tagBackStack)
                            .commit();
                    previous = fragmentToAdd;
                }
            }
        } else {
            Fragment current = fragmentManager.findFragmentById(R.id.fragmentContainer);
            fragmentManager.beginTransaction()
                    .hide(current)
                    .add(R.id.fragmentContainer, fragment, tagFragmentToAdd)
                    .addToBackStack(tagFragmentToAdd)
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return Navigation.findNavController(this, R.id.navHostFragment).navigateUp();
    }
}
