package com.general.files;

import android.content.Context;
import android.os.AsyncTask;

import com.google.firebase.iid.FirebaseInstanceId;
import com.utils.CommonUtilities;
import com.utils.Utils;
import com.view.MyProgressDialog;

import java.util.HashMap;

/**
 * Created by Admin on 22-02-2016.
 */
public class ExecuteWebServerUrl extends AsyncTask<String, String, String> {

    SetDataResponse setDataRes;

    HashMap<String, String> parameters;

    GeneralFunctions generalFunc;

    String responseString = "";

    boolean directUrl_value = false;
    String directUrl = "";

    boolean isLoaderShown = false;
    Context mContext;

    MyProgressDialog myPDialog;

    boolean isGenerateDeviceToken = false;
    String key_DeviceToken_param;
    InternetConnection intCheck;
    boolean isSetCancelable = true;

    public ExecuteWebServerUrl(Context mContext, HashMap<String, String> parameters) {
        this.parameters = parameters;
        this.mContext = mContext;
    }

    public ExecuteWebServerUrl(Context mContext, String directUrl, boolean directUrl_value) {
        this.directUrl = directUrl;
        this.directUrl_value = directUrl_value;
        this.mContext = mContext;
    }

    public void setLoaderConfig(Context mContext, boolean isLoaderShown, GeneralFunctions generalFunc) {
        this.isLoaderShown = isLoaderShown;
        this.generalFunc = generalFunc;
        this.mContext = mContext;
    }

    public void setIsDeviceTokenGenerate(boolean isGenerateDeviceToken, String key_DeviceToken_param) {
        this.isGenerateDeviceToken = isGenerateDeviceToken;
        this.key_DeviceToken_param = key_DeviceToken_param;
    }

    public void setIsDeviceTokenGenerate(boolean isGenerateDeviceToken, String key_DeviceToken_param, GeneralFunctions generalFunc) {
        this.isGenerateDeviceToken = isGenerateDeviceToken;
        this.key_DeviceToken_param = key_DeviceToken_param;
        this.generalFunc = generalFunc;
    }

    public void setCancelAble(boolean isSetCancelable) {
        this.isSetCancelable = isSetCancelable;

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // Utils.hideKeyboard(mContext);
        intCheck = new InternetConnection(mContext);

        if (!intCheck.isNetworkConnected() && !intCheck.check_int()) {

            return;
        }
        if (isLoaderShown == true) {
            myPDialog = new MyProgressDialog(mContext, isSetCancelable, generalFunc.retrieveLangLBl("Loading", "LBL_LOADING_TXT"));
            isSetCancelable = true;
            try {
                myPDialog.show();
            } catch (Exception e) {

            }
        }
    }

    @Override
    protected String doInBackground(String... strings) {
        if (isGenerateDeviceToken == true && generalFunc != null) {

            String vDeviceToken = generalFunc.generateDeviceToken();

            if (vDeviceToken.equals("")) {
                return "";
            }

            if (parameters != null) {
                parameters.put(key_DeviceToken_param, "" + vDeviceToken);
            }
        }

        if (parameters != null) {
            GeneralFunctions generalFunc = new GeneralFunctions(mContext);
            parameters.put("tSessionId", generalFunc.getMemberId().equals("") ? "" : generalFunc.retrieveValue(Utils.SESSION_ID_KEY));

            parameters.put("GeneralUserType", CommonUtilities.app_type);
            parameters.put("GeneralMemberId", generalFunc.getMemberId());
            parameters.put("GeneralDeviceType", "" + Utils.deviceType);
            parameters.put("GeneralAppVersion", Utils.getAppVersion());
            parameters.put("vTimeZone", generalFunc.getTimezone());

            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            if (refreshedToken != null && !refreshedToken.equals("")) {
                parameters.put("vFirebaseDeviceToken", refreshedToken);
            }
        }


        if (directUrl_value == false) {
            // responseString = OutputStreamReader.performPostCall(CommonUtilities.SERVER_URL_WEBSERVICE, parameters);
            responseString = OutputStreamReader.performPostCall(intCheck, CommonUtilities.SERVER_URL_WEBSERVICE, parameters);
        } else {
            responseString = new ExecuteResponse().getResponse(directUrl);
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        try {
            if (myPDialog != null) {
                myPDialog.close();
            }
        } catch (Exception e) {

        }

        if (setDataRes != null) {

            GeneralFunctions generalFunc = new GeneralFunctions(mContext);
//            String message = (generalFunc!=null && Utils.checkText(responseString) && generalFunc.checkDataAvail(CommonUtilities.message_str, responseString))?generalFunc.getJsonValue(CommonUtilities.message_str, responseString):null;
            String message = Utils.checkText(responseString) ? generalFunc.getJsonValue(CommonUtilities.message_str, responseString) : null;

           if (message != null && message.equals("DO_RESTART")) {
                generalFunc.restartApp();
               Utils.printLog("restartCall", "executewebserver");
                Utils.runGC();
                return;
            } else {
                setDataRes.setResponse(responseString);
            }
        }
    }

    public interface SetDataResponse {
        void setResponse(String responseString);
    }

    public void setDataResponseListener(SetDataResponse setDataRes) {
        this.setDataRes = setDataRes;
    }

}
