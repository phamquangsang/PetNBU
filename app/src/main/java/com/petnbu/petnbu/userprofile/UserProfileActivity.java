package com.petnbu.petnbu.userprofile;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ActivityUserProfileBinding;
import com.petnbu.petnbu.model.Feed;

public class UserProfileActivity extends AppCompatActivity implements UserProfileFragment.OnProfileFragmentInteractionListener{

    private static String ARG_USER_ID = "user-id";
    private ActivityUserProfileBinding mBinding;

    public static Intent newIntent(Context c, String userId){
        Intent i = new Intent(c, UserProfileActivity.class);
        i.putExtra(ARG_USER_ID, userId);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String userId = getIntent().getStringExtra(ARG_USER_ID);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, UserProfileFragment.newInstance(3, userId))
                .commit();
    }



    @Override
    public void onListFragmentInteractionListener(Feed item) {

    }
}
