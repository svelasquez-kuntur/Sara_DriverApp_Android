package com.sara.driver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.general.files.GeneralFunctions;
import com.general.files.MyApp;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.utils.CommonUtilities;
import com.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Admin on 29-07-2017.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //Displaying data in log
        //It is optional

        Utils.printLog("FCMCALL","::"+remoteMessage);
        Log.e(TAG, "From:" + remoteMessage.getData().toString());
        Log.d(TAG, "From: " + remoteMessage.getData().get("message"));
        Log.d(TAG, "From: " + remoteMessage.getData().toString());
//        Log.d(TAG, "From: " + remoteMessage.getNotification().getBody());
//        Log.d(TAG, "From: " + remoteMessage.getMessageType());
        if (remoteMessage == null || remoteMessage.getData() == null/* || remoteMessage.getNotification().getBody() == null*/)
            return;


        String message = remoteMessage.getData().get("message");
        Utils.printLog("FCMCALL","::"+message);
        GeneralFunctions generalFunc = new GeneralFunctions(this);

        if (generalFunc.getJsonValue("MsgType", remoteMessage.getData().get("message")).equals("CHAT")) {
            sendNotification(remoteMessage.getData().get("message"));
        } else {

            if (message != null && Utils.checkText(message)) {
                if (isJSONValid(message)) {

                    sendBroadCast(CommonUtilities.passenger_message_arrived_intent_action, message);
                    sendBroadCast(CommonUtilities.passenger_message_arrived_intent_action_trip_msg, message);

                } else {
                    try {
                        PowerManager powerManager = (PowerManager) MyApp.getCurrentAct().getSystemService(Context.POWER_SERVICE);
                        boolean isScreenOn = powerManager.isScreenOn();
                        if (isScreenOn) {
                            buildMessage(message);
                            Utils.generateNotification(getApplicationContext(), message);
                        } else {
                            Utils.generateNotification(getApplicationContext(), message);
                        }
                    } catch (Exception e) {
                        Utils.generateNotification(getApplicationContext(), message);

                    }
                }
            }
        }

    }

    public void sendBroadCast(String action, String message) {
        Intent intent_broad = new Intent(action);
        intent_broad.putExtra(CommonUtilities.passenger_message_arrived_intent_key, message);
        this.sendBroadcast(intent_broad);
    }


    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }


    public void buildMessage(final String message) {

        MyApp.getCurrentAct().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GeneralFunctions generalFunc = new GeneralFunctions(MyApp.getCurrentAct());
                generalFunc.showGeneralMessage("", message);

            }
        });

    }


    //This method is only generating push notification
    //It is same as we did in earlier posts
    private void sendNotification(String messageBody) {

        GeneralFunctions generalFunc = new GeneralFunctions(this);
        if (generalFunc.getJsonValue("MsgType", messageBody).equals("CHAT")) {

            try {

                Utils.printLog("CurrentClass", ":1:" + MyApp.getCurrentAct().getClass().getSimpleName());
                Utils.printLog("CurrentClass", "::" + Utils.getPreviousIntent(this).getComponent().getClassName());
            } catch (Exception e) {

            }
            if (Utils.getPreviousIntent(this) != null) {

                //  new OpenChatDetailDialog(this, driverDetailFrag.getTripData(), generalFunc, "");

                Bundle bn = new Bundle();

                bn.putString("iFromMemberId", generalFunc.getJsonValue("iFromMemberId", messageBody));
                bn.putString("FromMemberImageName", generalFunc.getJsonValue("FromMemberImageName", messageBody));
                bn.putString("iTripId", generalFunc.getJsonValue("iTripId", messageBody));
                bn.putString("FromMemberName", generalFunc.getJsonValue("FromMemberName", messageBody));


                if (getApp().isMyAppInBackGround() == true) {

                    if (MyApp.getCurrentAct().getClass().getSimpleName().equals("ChatActivity")) {
                        Utils.generateNotification(this, generalFunc.getJsonValue("Msg", messageBody));
                    } else {

                        Utils.printLog("firbaseNotification", "call");
                        Intent intent = new Intent(this, ChatActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("isnotification", true);
                        intent.putExtras(bn);
                        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(getApplicationContext().getResources().getString(R.string.app_name))
                                .setContentText(generalFunc.getJsonValue("Msg", messageBody))
                                .setAutoCancel(true)
                                .setSound(defaultSoundUri)
                                .setContentIntent(pendingIntent);

                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        notificationManager.notify(0, notificationBuilder.build());

                    }


                } else {

                    if (MyApp.getCurrentAct().getClass().getSimpleName().equals("ChatActivity")) {
                        Utils.generateNotification(this, generalFunc.getJsonValue("Msg", messageBody));
                        return;
                    }

                    Intent show_timer = new Intent();
                    show_timer.setClass(this, ChatActivity.class);
                    show_timer.putExtras(bn);

                    show_timer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(show_timer);

                    // new StartActProcess(this).startActWithData(ChatActivity.class, bn);
                }

            } else {
                generalFunc.storedata("OPEN_CHAT", "Yes");

                Utils.generateNotification(this, generalFunc.getJsonValue("Msg", messageBody));
            }


        }

    }

    public MyApp getApp() {
        return ((MyApp) getApplication());
    }
}