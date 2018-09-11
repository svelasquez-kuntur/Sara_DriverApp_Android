package com.general.files;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.sara.driver.ActiveTripActivity;
import com.sara.driver.MyProfileActivity;
import com.sara.driver.UploadDocActivity;
import com.utils.Utils;
import com.view.MyProgressDialog;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;

public class UploadProfileImage extends AsyncTask<String, String, String> {

    String selectedImagePath;
    String responseString = "";

    String temp_File_Name = "";
    ArrayList<String[]> paramsList;
    String type = "";
    Context actContext;
    MyProgressDialog myPDialog;
    GeneralFunctions generalFunc;

    public UploadProfileImage(Context actContext, String selectedImagePath, String temp_File_Name, ArrayList<String[]> paramsList, String type) {
        this.selectedImagePath = selectedImagePath;
        this.temp_File_Name = temp_File_Name;
        this.paramsList = paramsList;
        this.type = type;
        this.actContext = actContext;
        this.generalFunc = new GeneralFunctions(actContext);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        myPDialog = new MyProgressDialog(actContext, false, generalFunc.retrieveLangLBl("Loading", "LBL_LOADING_TXT"));
        try {
            myPDialog.show();
        } catch (Exception e) {

        }
    }

    @Override
    protected String doInBackground(String... strings) {
        String filePath = selectedImagePath;
        if (TextUtils.isEmpty(type)) {
            filePath = generalFunc.decodeFile(selectedImagePath, Utils.ImageUpload_DESIREDWIDTH,
                    Utils.ImageUpload_DESIREDHEIGHT, temp_File_Name);
        }
        try {
            responseString = new ExecuteResponse().uploadImageAsFile(filePath, temp_File_Name,"vImage", paramsList);
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        try {
            myPDialog.close();
        } catch (Exception e) {

        }
        if (actContext instanceof MyProfileActivity) {
            ((MyProfileActivity) actContext).handleImgUploadResponse(responseString);
        } else if (actContext instanceof ActiveTripActivity) {
            ((ActiveTripActivity) actContext).handleImgUploadResponse(responseString, type);
        }else if (actContext instanceof UploadDocActivity) {
            ((UploadDocActivity) actContext).handleImgUploadResponse(responseString);
        }
    }
}
