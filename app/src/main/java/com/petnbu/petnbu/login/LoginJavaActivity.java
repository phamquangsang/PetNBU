package com.petnbu.petnbu.login;

<<<<<<< HEAD
import android.app.DialogFragment;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
=======
>>>>>>> 842164d70d2760d8e8d8761246f4f9446c3e8cee
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.MainActivity;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.SuccessCallback;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.databinding.ActivityLoginJavaBinding;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.User;

import javax.inject.Inject;

import timber.log.Timber;

public class LoginJavaActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LoginJavaActivity.class.getSimpleName();
    private ActivityLoginJavaBinding mBinding;

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Inject
    WebService mWebService;
    @Inject
    AppExecutors mAppExecutors;

    @Inject
    UserDao mUserDao;

    @Inject
    PetDb mPetDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PetApplication.getAppComponent().inject(this);
//        PetDb db = Room.databaseBuilder(getApplication(), PetDb.class, "pet.db").build();;
//        mUserDao = mPetDb.userDao();

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_login_java);
        mBinding.btnGoogleSignIn.setOnClickListener(this);
        mBinding.btnGoogleSignOut.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out d
        mGoogleSignInClient.signOut().addOnCompleteListener(this,  dtask -> updateUI(null));
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_google_sign_in) {
            signIn();
        } else if (i == R.id.btn_google_sign_out) {
            signOut();
        }
//        else if (i == R.id.disconnect_button) {
//            revokeAccess();
//        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        showProgressbar();
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Snackbar.make(mBinding.getRoot(), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void openMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void updateUI(FirebaseUser firebaseUser) {
        hideProgressDialog();
        if (firebaseUser != null) {
            mBinding.tvStatus.setText(String.format("logged in as %s", firebaseUser.getEmail()));
            mBinding.tvDetail.setText(String.format("user id: %s", firebaseUser.getUid()));

            mBinding.btnGoogleSignIn.setVisibility(View.GONE);
            mBinding.btnGoogleSignOut.setVisibility(View.VISIBLE);
            createUser(firebaseUser);
        } else {
            mBinding.tvStatus.setText(R.string.signed_out);
            mBinding.tvDetail.setText(null);
            mBinding.btnGoogleSignIn.setVisibility(View.VISIBLE);
            mBinding.btnGoogleSignOut.setVisibility(View.GONE);
        }
    }

    private void createUser(FirebaseUser firebaseUser) {
        String photoUrl = "";
        if(firebaseUser.getPhotoUrl() != null){
            photoUrl = firebaseUser.getPhotoUrl().toString();
        }
        Photo photo = new Photo(photoUrl, null, null, null, 0, 0);
        User user = new User(firebaseUser.getUid(), photo, firebaseUser.getDisplayName(), firebaseUser.getEmail(), null, null);
        mWebService.getUser(firebaseUser.getUid(), new SuccessCallback<User>() {
            @Override
            public void onSuccess(User user) {
                //user existed -> go to Main screen
                SharedPrefUtil.saveUserId(getApplicationContext(), user.getUserId());
                mAppExecutors.diskIO().execute(() -> mUserDao.insert(user));
                openMainActivity();
            }

            @Override
            public void onFailed(Exception e) {
                mWebService.createUser(user, new SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        SharedPrefUtil.saveUserId(getApplicationContext(), user.getUserId());
                        openMainActivity();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Snackbar.make(mBinding.getRoot(), "Error: "+e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void hideProgressDialog() {
        mBinding.progressBar.setVisibility(View.GONE);
    }

    private void showProgressbar() {
        mBinding.progressBar.setVisibility(View.VISIBLE);
    }

}
