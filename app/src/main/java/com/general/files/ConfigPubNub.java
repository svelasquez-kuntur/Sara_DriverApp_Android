package com.general.files;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.sara.driver.CabRequestedActivity;
import com.sara.driver.MainActivity;
import com.sara.driver.R;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNLogVerbosity;
import com.pubnub.api.enums.PNReconnectionPolicy;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.utils.CommonUtilities;
import com.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Admin on 05-10-2016.
 */
public class ConfigPubNub implements GetLocationUpdates.LocationUpdates, UpdateFrequentTask.OnTaskRunCalled {
    public boolean isSubsToCabReq = false;
    public boolean isPubnubInstKilled = false;
    public Location driverLoc = null;
    Context mContext;
    PubNub pubnub;
    GeneralFunctions generalFunc;
    UpdateFrequentTask updatepubnubtask;
    UpdateFrequentTask updateprivatetask;
    private InternetConnection intCheck;


    SubscribeCallback subscribeCallback = new SubscribeCallback() {
        @Override
        public void status(final PubNub pubnub, final PNStatus status) {
            // the status object returned is always related to subscribe but could contain
            // information about subscribe, heartbeat, or errors
            // use the operationType to switch on different options
//            Utils.printLog("status operation", ":::re connected::" + status.toString());
            if (status == null || status.getOperation() == null) {
//                 Utils.printLog("status operation", ":::re connected::" + status.toString());
                connectTopubNub();
                return;
            }

            switch (status.getOperation()) {
                // let's combine unsubscribe and subscribe handling for ease of use
                case PNSubscribeOperation:
                case PNUnsubscribeOperation:
                    // note: subscribe statuses never have traditional
                    // errors, they just have categories to represent the
                    // different issues or successes that occur as part of subscribe
                    switch (status.getCategory()) {
                        case PNConnectedCategory:
                            // this is expected for a subscribe, this means there is no error or issue whatsoever
//                             Utils.printLog("PNConnectedCategory", ":::connected::" + status.toString());

                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_Connected);
                            }
                        case PNReconnectedCategory:
                            // this usually occurs if subscribe temporarily fails but reconnects. This means
                            // there was an error but there is no longer any issue
//                             Utils.printLog("PNReconnectedCategory", ":::re connected::" + status.toString());
                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_Connected);
                            }
                        case PNDisconnectedCategory:
                            // this is the expected category for an unsubscribe. This means there
                            // was no error in unsubscribing from everything
//                             Utils.printLog("PNDisconnectedCategory", ":::dis connected::" + status.toString());
                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_DisConnected);
                            }
                        case PNTimeoutCategory:
//                            Utils.printLog("TimeOut", ":::time out::" + status.toString());

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
//                                    pubnub.reconnect();
                                    connectTopubNub();
//                                     Utils.printLog("PNUnexpectedDisconnect", ":::dis unexpected::" + status.toString());
                                }
                            }, 1500);

                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_DisConnected);
                            }

                        case PNUnexpectedDisconnectCategory:
                            // this is usually an issue with the internet connection, this is an error, handle appropriately
                            // retry will be called automatically
//                            Utils.printLog("UnExpected Disconnect", ":::unexpected disconnect::" + status.toString());
//                            Utils.printLog("UnExpected Disconnect", ":::unexpected disconnect::" + status.isError());

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
//                                    pubnub.reconnect();
                                    connectTopubNub();
//                                     Utils.printLog("PNUnexpectedDisconnect", ":::dis unexpected::" + status.toString());
                                }
                            }, 1500);

//                             Utils.printLog("PNUnexpectedDisconnect", ":::dis unexpected::" + status.toString());
                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_DisConnected);
                            }
                        case PNAccessDeniedCategory:
                            // this means that PAM does allow this client to subscribe to this
                            // channel and channel group configuration. This is another explicit error
//                            Utils.printLog("AccessDenied", ":::denied::" + status.toString());
                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_Denied);
                            }
                        case PNDecryptionErrorCategory:
//                            Utils.printLog("Default", ":::PNDecryptionErrorCategory::" + status.toString());
                        default:
                            // More errors can be directly specified by creating explicit cases for other
                            // error categories of `PNStatusCategory` such as `PNTimeoutCategory` or `PNMalformedFilterExpressionCategory` or `PNDecryptionErrorCategory`
//                            Utils.printLog("Default", ":::default::" + status.toString());
                            if (mContext instanceof MainActivity) {
                                ((MainActivity) mContext).pubNubStatus(Utils.pubNubStatus_Error_Connection);
                            }
                    }

                case PNHeartbeatOperation:
                    // heartbeat operations can in fact have errors, so it is important to check first for an error.
                    // For more information on how to configure heartbeat notifications through the status
                    // PNObjectEventListener callback, consult <link to the PNCONFIGURATION heartbeart config>
                    if (status.isError()) {
                        // There was an error with the heartbeat operation, handle here
//                         Utils.printLog("PNHeartbeatOperation", ":::failed::" + status.toString());
                    } else {
                        // heartbeat operation was successful
//                         Utils.printLog("PNHeartbeatOperation", ":::success::" + status.toString());
                    }
                default: {
                    // Encountered unknown status type
//                    Utils.printLog("unknown status", ":::unknown::" + status.toString());
                }
            }
        }

        @Override
        public void message(PubNub pubnub, PNMessageResult message) {
            // handle incoming messages
            Utils.printLog("Intialize", "MsgOnPubNub");
            Utils.printLog("ON message", ":::got::" + message.getMessage());


            /*String msg = message.getMessage().toString().replace("\\\"", "\"");
            String finalMsg = "";
            if (isJsonObj(message.getMessage().toString())) {
                finalMsg = message.getMessage().toString();
            } else {
                finalMsg = msg.substring(1, msg.length() - 1);
            }

            dispatchMsg(finalMsg);*/
            dispatchMsg(message.getMessage().toString());

        }

        @Override
        public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            // handle incoming presence data
//            Utils.printLog("ON presence", ":::got::" + presence.toString());
        }
    };
    private GetLocationUpdates getLocUpdates;
    private UpdateFrequentTask updatedriverStatustask;
    private String iTripId = "";
    private String PassengerId = "";
    private ExecuteWebServerUrl currentExeTask;


    public ConfigPubNub(Context mContext) {
        this.mContext = mContext;

        generalFunc = new GeneralFunctions(mContext);
        intCheck=new InternetConnection(mContext);

        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setUuid(generalFunc.retrieveValue(Utils.DEVICE_SESSION_ID_KEY).equals("") ? generalFunc.getMemberId() : generalFunc.retrieveValue(Utils.DEVICE_SESSION_ID_KEY));
        pnConfiguration.setSubscribeKey(generalFunc.retrieveValue(CommonUtilities.PUBNUB_SUB_KEY));
        pnConfiguration.setPublishKey(generalFunc.retrieveValue(CommonUtilities.PUBNUB_PUB_KEY));
        pnConfiguration.setSecretKey(generalFunc.retrieveValue(CommonUtilities.PUBNUB_SEC_KEY));
        pnConfiguration.setReconnectionPolicy(PNReconnectionPolicy.LINEAR);
//        pnConfiguration.setLogVerbosity(PNLogVerbosity.BODY);

        pubnub = new PubNub(pnConfiguration);
        addListener();
        getPassenegerMsgPubNubOff();
        subscribeToPrivateChannel();
        reConnectPubNub(2000);
        reConnectPubNub(5000);
        reConnectPubNub(10000);

    }

    public ConfigPubNub(Context mContext, boolean isOnlyPublish) {
        this.mContext = mContext;
        generalFunc = new GeneralFunctions(mContext);
        intCheck=new InternetConnection(mContext);

        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(generalFunc.retrieveValue(CommonUtilities.PUBNUB_SUB_KEY));
        pnConfiguration.setPublishKey(generalFunc.retrieveValue(CommonUtilities.PUBNUB_PUB_KEY));
        pnConfiguration.setSecretKey(generalFunc.retrieveValue(CommonUtilities.PUBNUB_SEC_KEY));
        pnConfiguration.setReconnectionPolicy(PNReconnectionPolicy.LINEAR);
//        pnConfiguration.setLogVerbosity(PNLogVerbosity.BODY);

        pubnub = new PubNub(pnConfiguration);
        addListener();

        getPassenegerMsgPubNubOff();

        subscribeToPrivateChannel();
        reConnectPubNub(2000);
        reConnectPubNub(5000);
        reConnectPubNub(10000);
    }

    public void reConnectPubNub(int duration) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Utils.printLog("Presence:", "::" + pubnub.getPresenceState());
                connectTopubNub();
            }
        }, duration);
    }

    public void getPassenegerMsgPubNubOff() {

        if (getLocUpdates == null) {

            getLocUpdates = new GetLocationUpdates(mContext, Utils.LOCATION_UPDATE_MIN_DISTANCE_IN_MITERS, true);
            getLocUpdates.setLocationUpdatesListener(this);

        }

        if (updatedriverStatustask == null) {

            Utils.printLog("FETCH_TRIP_STATUS_TIME_INTERVAL_KEY", "::" + generalFunc.retrieveValue(Utils.FETCH_TRIP_STATUS_TIME_INTERVAL_KEY));
            updatedriverStatustask = new UpdateFrequentTask(generalFunc.parseIntegerValue(15, generalFunc.retrieveValue(Utils.FETCH_TRIP_STATUS_TIME_INTERVAL_KEY)) * 1000);
            updatedriverStatustask.setTaskRunListener(this);
            updatedriverStatustask.startRepeatingTask();
        }

    }

    public void connectTopubNub() {
        isPubnubInstKilled = false;
        pubnub.reconnect();
    }

    public void setTripId(String iTripId, String PassengerId) {
        this.iTripId = iTripId;
        this.PassengerId = PassengerId;

    }

    public void subscribeToPrivateChannel() {
        pubnub.subscribe()
                .channels(Arrays.asList("DRIVER_" + generalFunc.getMemberId())) // subscribe to channels
                .execute();

        if (updateprivatetask != null) {
            updateprivatetask.stopRepeatingTask();
            updateprivatetask = null;
        }
        updateprivatetask = new UpdateFrequentTask(14 * 1000);
        updateprivatetask.setTaskRunListener(new UpdateFrequentTask.OnTaskRunCalled() {
            @Override
            public void onTaskRun() {


                pubnub.subscribe()
                        .channels(Arrays.asList("DRIVER_" + generalFunc.getMemberId())) // subscribe to channels
                        .execute();

                generalFunc.sendHeartBeat();

            }
        });
        updateprivatetask.startRepeatingTask();
    }

    public void unSubscribeToPrivateChannel() {
        pubnub.unsubscribe()
                .channels(Arrays.asList("DRIVER_" + generalFunc.getMemberId())) // subscribe to channels
                .execute();

        if (updateprivatetask != null) {
            updateprivatetask.stopRepeatingTask();
            updateprivatetask = null;
        }
    }

    public void releaseInstances() {

        pubnub.removeListener(subscribeCallback);
        isPubnubInstKilled = true;

        removeRunningInstance();
        Utils.printLog("Intialize", "Release PubNub");
        Utils.runGC();
    }

    private void removeRunningInstance() {
        if (updatepubnubtask != null) {
            updatepubnubtask.stopRepeatingTask();
            updatepubnubtask = null;
        }

        if (updateprivatetask != null) {
            updateprivatetask.stopRepeatingTask();
            updateprivatetask = null;
        }
        if (updatedriverStatustask != null) {
            updatedriverStatustask.stopRepeatingTask();
            updatedriverStatustask = null;
        }


        if (getLocUpdates != null) {
            getLocUpdates.stopLocationUpdates();
            getLocUpdates.removeLocUpdateListener();
            getLocUpdates = null;
        }
        Utils.printLog("Intialize", "Remove Instances");

    }

    public void addListener() {
        Utils.printLog("Intialize", "addListener");
        releaseInstances();
        pubnub.addListener(subscribeCallback);
        connectTopubNub();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                connectTopubNub();
            }
        }, 10000);
    }

    public boolean isJsonObj(String json) {

        try {
            JSONObject obj_check = new JSONObject(json);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void dispatchMsg(String jsonMsg) {
        String finalMsg = jsonMsg;

        if (!isJsonObj(finalMsg)) {
            finalMsg = jsonMsg.replaceAll("\\\\", "");
//        finalMsg = finalMsg.replace("\"","\\\"");
//        finalMsg = finalMsg.substring(1,finalMsg.length() - 1);
//        String finalMsg = convertStandardJSONString(jsonMsg);

            finalMsg = finalMsg.replaceAll("^\"|\"$", "");

            if (!isJsonObj(finalMsg)) {
                finalMsg = jsonMsg.replace("\\\"", "\"").replaceAll("^\"|\"$", "");
            }

            finalMsg = finalMsg.replace("\\\\\"", "\\\"");
        }


        Utils.printLog("finalMsg", "::" + finalMsg);

        String codeKey = CommonUtilities.DRIVER_REQ_CODE_PREFIX_KEY + generalFunc.getJsonValue("MsgCode", finalMsg);

        if (generalFunc.retrieveValue(codeKey).equals("") && !generalFunc.containsKey(CommonUtilities.DRIVER_REQ_COMPLETED_MSG_CODE_KEY + generalFunc.getJsonValue("MsgCode", finalMsg))) {
            Utils.sendBroadCast(mContext, CommonUtilities.passenger_message_arrived_intent_action, finalMsg);
        }

        Utils.sendBroadCast(mContext, CommonUtilities.passenger_message_arrived_intent_action_trip_msg, finalMsg);
    }

    public void subscribeToCabRequestChannel() {
        isSubsToCabReq = true;

        pubnub.subscribe()
                .channels(Arrays.asList("CAB_REQUEST_DRIVER_" + generalFunc.getMemberId())) // subscribe to channels
                .execute();

        if (updatepubnubtask != null) {
            updatepubnubtask.stopRepeatingTask();
            updatepubnubtask = null;
        }
        updatepubnubtask = new UpdateFrequentTask(12 * 1000);
//            this.updateRequest = updateRequest;
        updatepubnubtask.setTaskRunListener(new UpdateFrequentTask.OnTaskRunCalled() {
            @Override
            public void onTaskRun() {
                pubnub.subscribe()
                        .channels(Arrays.asList("CAB_REQUEST_DRIVER_" + generalFunc.getMemberId())) // subscribe to channels
                        .execute();

                generalFunc.sendHeartBeat();
            }
        });
        updatepubnubtask.startRepeatingTask();
    }


    public void unSubscribeToCabRequestChannel() {
        isSubsToCabReq = false;
        Utils.printLog("Online", "UnSub");

        pubnub.unsubscribe()
                .channels(Arrays.asList("CAB_REQUEST_DRIVER_" + generalFunc.getMemberId())) // subscribe to channels
                .execute();

        if (updatepubnubtask != null) {
            updatepubnubtask.stopRepeatingTask();
            updatepubnubtask = null;
        }
    }


    public void publishMsg(String channel, String message) {
        if (message == null) {
            Utils.printLog("Msg null", "Yes");
            return;
        }
        pubnub.publish()
                .message(message)
                .channel(channel)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        // handle publish result, status always present, result if successful
                        // status.isError to see if error happened
                        if (result != null && result.getTimetoken() != null) {
                            Utils.printLog("Publish Res", "::::" + result.getTimetoken());
                        }
                        if (status.isError()) {
                            Utils.printLog("Msg isError", "" + status);
                        } else {
                            Utils.printLog("Msg null", "Published!");
                        }
                    }
                });
    }


    private void getUpdatedDriverStatus() {

        if (!intCheck.isNetworkConnected() && !intCheck.check_int()) {

            return;
        }


        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("type", "configDriverTripStatus");
        parameters.put("iTripId", iTripId);
        parameters.put("iMemberId", generalFunc.getMemberId());
        parameters.put("UserType", Utils.userType);

        if (driverLoc != null) {
            parameters.put("vLatitude", "" + driverLoc.getLatitude());
            parameters.put("vLongitude", "" + driverLoc.getLongitude());
        }

        parameters.put("isSubsToCabReq", "" + isSubsToCabReq);

        /*if (currentExeTask != null) {
            currentExeTask.cancel(true);
            currentExeTask = null;
        }*/

        if (this.currentExeTask != null) {
            this.currentExeTask.cancel(true);
            this.currentExeTask = null;
            Utils.runGC();
        }

        ExecuteWebServerUrl exeWebServer = new ExecuteWebServerUrl(mContext, parameters);
        this.currentExeTask = exeWebServer;
        exeWebServer.setDataResponseListener(new ExecuteWebServerUrl.SetDataResponse() {
            @Override
            public void setResponse(String responseString) {

                if (responseString != null && Utils.checkText(responseString)) {
                  //  Utils.printLog("Api", "configDriverTripStatus ::" + responseString);

                    boolean isDataAvail = generalFunc.checkDataAvail(CommonUtilities.action_str, responseString);

                    if (isDataAvail == true && isPubnubInstKilled == false) {

                        if (!iTripId.isEmpty()) {
                            dispatchMsg(generalFunc.getJsonValue(CommonUtilities.message_str, responseString));

                        } else {

                            JSONArray msgArr = generalFunc.getJsonArray(CommonUtilities.message_str, responseString);

                            if (msgArr != null) {
                                for (int i = 0; i < msgArr.length(); i++) {

                                    String tempStr = ((String) generalFunc.getValueFromJsonArr(msgArr, i)).replaceAll("^\"|\"$", "");

                                    Utils.printLog("Orig", "::" + tempStr);
//                                    JSONObject obj_temp1 = generalFunc.getJsonObjectFromString(obj_temp.toString());

//                                    if (obj_temp1 != null) {
//                                        Utils.printLog("Api", "obj_temp::" + obj_temp1.toString().replace("[\"", "").replace("\"]", "").replace("\\\"", "\""));
                                    dispatchMsg(tempStr);
//                                    }

                                }
                            }


                        }
                    }
                }
            }
        });
        exeWebServer.execute();
    }


    public String getMessageFromStringObj(String message) {
        String msg = message.toString().replace("\\\"", "\"");
        String finalMsg = "";
        if (isJsonObj(message.toString())) {
            finalMsg = message.toString();
        } else {
            finalMsg = msg.substring(1, msg.length() - 1);
        }

        return finalMsg;
    }

    @Override
    public void onLocationUpdate(Location location) {
        if (location == null) {
            return;
        }
        this.driverLoc = location;

    }

    @Override
    public void onTaskRun() {
        Utils.runGC();
        Utils.printLog("Caller", "::" + mContext.getClass().getSimpleName());
        Utils.printLog("Caller", ":1:" + MyBackGroundService.class.getSimpleName());

        if (mContext.getClass().getSimpleName().equals(MyBackGroundService.class.getSimpleName())) {
            if (MyApp.getInstance().isMyAppInBackGround() == true) {

                Utils.printLog("Caller", ":2:" + mContext.getClass().getSimpleName() + "::Interval::" + updatedriverStatustask.mInterval);
                getUpdatedDriverStatus();
            }
        } else {
            if (MyApp.getInstance().isMyAppInBackGround() == false) {

                Utils.printLog("Caller", ":3:" + mContext.getClass().getSimpleName() + "::Interval::" + updatedriverStatustask.mInterval);
                getUpdatedDriverStatus();
            }

        }
    }
}
