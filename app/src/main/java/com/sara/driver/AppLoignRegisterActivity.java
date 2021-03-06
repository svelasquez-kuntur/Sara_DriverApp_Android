package com.sara.driver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.login.widget.LoginButton;
import com.fragments.SignInFragment;
import com.fragments.SignUpFragment;
import com.general.files.GeneralFunctions;
import com.general.files.InternetConnection;
import com.general.files.RegisterFbLoginResCallBack;
import com.general.files.RegisterGoogleLoginResCallBack;
import com.general.files.RegisterTwitterLoginResCallBack;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.utils.CommonUtilities;
import com.utils.Utils;
import com.view.MTextView;

import java.util.Arrays;


public class AppLoignRegisterActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public MTextView titleTxt;
    ImageView backImgView;
    public GeneralFunctions generalFunc;
    FrameLayout container;
    String type = "";

    public MTextView signheaderHint,orTxt;

    ImageView imagefacebook,imagetwitter,imageGoogle;

    CallbackManager callbackManager;
    LoginButton loginButton;

    GoogleApiClient mGoogleApiClient;

    private static final int RC_SIGN_IN = 001;

    static String TWITTER_CONSUMER_KEY = ""; // place your cosumer key here
    static String TWITTER_CONSUMER_SECRET = ""; // place your consumer secret here

    private TwitterLoginButton twitterloginButton;
    SignUpFragment signUpFragment;
    private InternetConnection intCheck;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FacebookSdk.sdkInitialize(getActContext());
            FacebookSdk.setWebDialogTheme(R.style.FBDialogtheme);
            setContentView(R.layout.activity_app_loign_register);
            type = getIntent().getStringExtra("type");
            intCheck = new InternetConnection(this);
//
//            TwitterAuthConfig authConfig = new TwitterAuthConfig("9DLqGZaJLjDAFeEw9YagJ1xdV", "w2QYHlDQnK75jEjpqFv28pJfcbLGr2YTMRIJYdCLgLWPVeoMzx");

            TwitterAuthConfig authConfig = new TwitterAuthConfig(getActContext().getResources().getString(R.string.com_twitter_sdk_android_CONSUMER_KEY), getActContext().getResources().getString(R.string.com_twitter_sdk_android_CONSUMER_SECRET));

            TwitterConfig config = new TwitterConfig.Builder(this)
                    .logger(new DefaultLogger(Log.DEBUG))
                    .twitterAuthConfig(authConfig)
                    .debug(true)
                    .build();
            Twitter.initialize(config);
         //   Fabric.with(getActContext(), new TwitterCore(authConfig), new TweetUi());


            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(getActContext())
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();


            callbackManager = CallbackManager.Factory.create();
            initview();
            setLabel();
        }
        catch (Exception e) {

         Utils.printLog("exception oncreate()",e.toString());
        }

    }


    private void initview() {

        generalFunc = new GeneralFunctions(getActContext());
        FacebookSdk.setApplicationId(generalFunc.retrieveValue(CommonUtilities.FACEBOOK_APPID_KEY));
        titleTxt = (MTextView) findViewById(R.id.titleTxt);
        backImgView = (ImageView) findViewById(R.id.backImgView);
        backImgView.setOnClickListener(new setOnClickList());
        container = (FrameLayout) findViewById(R.id.container);

        signheaderHint=(MTextView)findViewById(R.id.signheaderHint);
        orTxt=(MTextView)findViewById(R.id.orTxt);


        imagefacebook=(ImageView)findViewById(R.id.imagefacebook);
        imagetwitter=(ImageView)findViewById(R.id.imagetwitter);
        imageGoogle=(ImageView)findViewById(R.id.imageGoogle);

        imagefacebook.setOnClickListener(new setOnClickList());
        imagetwitter.setOnClickListener(new setOnClickList());
        imageGoogle.setOnClickListener(new setOnClickList());



        loginButton = new LoginButton(getActContext());

        twitterloginButton=new TwitterLoginButton(getActContext());
        twitterloginButton.setCallback(new RegisterTwitterLoginResCallBack(getActContext()));


        callbackManager = CallbackManager.Factory.create();

        loginButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_friends", "user_about_me"));

        loginButton.registerCallback(callbackManager, new RegisterFbLoginResCallBack(getActContext(),callbackManager));

    }

    public Context getActContext() {
        return AppLoignRegisterActivity.this;
    }


    private void setLabel() {

        if (type != null) {
            if (type.equals("login")) {
                titleTxt.setText(generalFunc.retrieveLangLBl("", "LBL_SIGN_IN_TXT"));
                signheaderHint.setText(generalFunc.retrieveLangLBl("","LBL_SIGN_IN_WITH_SOC_ACC"));

                hadnleFragment(new SignInFragment());

            } else {
                titleTxt.setText(generalFunc.retrieveLangLBl("", "LBL_SIGN_UP"));
                signheaderHint.setText(generalFunc.retrieveLangLBl("","LBL_SIGN_UP_WITH_SOC_ACC"));
                hadnleFragment(new SignUpFragment());
            }
        }

        orTxt.setText(generalFunc.retrieveLangLBl("","LBL_OR_TXT"));


    }

    public void hadnleFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public class setOnClickList implements View.OnClickListener {

        @Override
        public void onClick(View view) {

            int i=view.getId();
            Utils.hideKeyboard(AppLoignRegisterActivity.this);
            if(i==backImgView.getId())
            {
                finish();

            }
           else  if(i== imagefacebook.getId())
            {
                if (!intCheck.isNetworkConnected() && !intCheck.check_int()) {

                    generalFunc.showError();
                }
                else {
                    loginButton.performClick();
                }

            }
            else if(i==imagetwitter.getId())
            {


                if (!intCheck.isNetworkConnected() && !intCheck.check_int()) {

                    generalFunc.showError();
                }
                else {
                    twitterloginButton.performClick();
                }

            }
            else if(i==imageGoogle.getId())
            {
                if (!intCheck.isNetworkConnected() && !intCheck.check_int()) {

                    generalFunc.showError();
                }
                else {

                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }

            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            //handleSignInResult(result);

            new RegisterGoogleLoginResCallBack(getActContext()).handleSignInResult(result);
        }
        else if(requestCode== Utils.SELECT_COUNTRY_REQ_CODE)
        {
             SignUpFragment.setdata(requestCode,resultCode,data);
        }
        else if(requestCode==140)
        {
            // twitterloginButton.setCallback(new RegisterTwitterLoginResCallBack(getActContext()));
            twitterloginButton.onActivityResult(requestCode, resultCode, data);

//            TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
//            TwitterAuthToken authToken = session.getAuthToken();
//            String token = authToken.token;
//            String secret = authToken.secret;
//
//            TwitterAuthClient authClient = new TwitterAuthClient();
//            authClient.requestEmail(session, new Callback<String>() {
//
//
//                @Override
//                public void success(Result<String> result) {
//                    // Do something with the result, which provides the email address
//                    Utils.printLog("sucess","");
//
//                }
//
//                @Override
//                public void failure(TwitterException exception) {
//                    // Do something on failure
//                    Utils.printLog("failure","");
//                }
//            });

        }
        else
        {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
