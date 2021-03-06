package com.sara.driver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.general.files.InternetConnection;
import com.general.files.MyApp;
import com.splunk.mint.MintActivity;

/**
 * Created by Admin on 31-08-2017.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {


    private MyApp mApplication;

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean status = new InternetConnection(context).isNetworkConnected();

        mApplication = ((MyApp) context.getApplicationContext());



        try {
            Activity currentActivity = mApplication.getCurrentActivity();

           //                if (!status) {
//
//                    ((MainActivity) currentActivity).handleNonetworkDial();
//
//                } else {
//                    ((MainActivity) currentActivity).noNetworkDialDismiss();
//                }

            if(currentActivity instanceof MintActivity)
            {
                ((MainActivity) currentActivity).handleNoNetworkDial();

            }
            else if(currentActivity instanceof ActiveTripActivity)
            {
                ((ActiveTripActivity) currentActivity).handleNoNetworkDial();

            }

        }
           catch (Exception e) {

        }
//        Toast.makeText(context, status, Toast.LENGTH_LONG).show();
    }
}