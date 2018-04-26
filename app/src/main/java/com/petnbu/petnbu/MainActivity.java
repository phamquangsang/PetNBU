package com.petnbu.petnbu;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.petnbu.petnbu.databinding.ActivityMainBinding;
import com.petnbu.petnbu.feed.FeedsViewModel;
import com.petnbu.petnbu.login.LoginJavaActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

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
    }
}
