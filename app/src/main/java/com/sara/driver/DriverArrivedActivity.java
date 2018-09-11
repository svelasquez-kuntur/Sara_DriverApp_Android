package com.sara.driver;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import com.general.files.CancelTripDialog;
import com.general.files.ConfigPubNub;
import com.general.files.ExecuteWebServerUrl;
import com.general.files.GeneralFunctions;
import com.general.files.GetAddressFromLocation;
import com.general.files.GetLocationUpdates;
import com.general.files.InternetConnection;
import com.general.files.OpenPassengerDetailDialog;
import com.general.files.StartActProcess;
import com.general.files.TripMessageReceiver;
import com.general.files.UpdateDirections;
import com.general.files.UpdateDriverLocationService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.SphericalUtil;
import com.utils.AnimateMarker;
import com.utils.CommonUtilities;
import com.utils.Utils;
import com.view.GenerateAlertBox;
import com.view.MButton;
import com.view.MTextView;
import com.view.MaterialRippleLayout;

import java.util.HashMap;

public class DriverArrivedActivity extends AppCompatActivity implements OnMapReadyCallback, GetLocationUpdates.LocationUpdates {

    public String tripId = "";
    public ImageView emeTapImgView;
    public MTextView timeTxt;
    GeneralFunctions generalFunc;
    MTextView titleTxt;
    MButton btn_type2;
    HashMap<String, String> data_trip;
    SupportMapFragment map;
    GoogleMap gMap;
    GetLocationUpdates getLocationUpdates;
    Location userLocation;
    ConfigPubNub configPubNub;
    TripMessageReceiver tripMsgReceiver;
    Intent startLocationUpdateService;
    MTextView addressTxt;
    Polyline route_polyLine;
    ExecuteWebServerUrl routeExeWebServer;
    boolean killRouteDrawn = false;
    String REQUEST_TYPE = "";
    android.support.v7.app.AlertDialog list_navigation;
    // public MTextView timeTxt;
    UpdateDirections updateDirections;
    Marker driverMarker;
    boolean isnotification = false;

    RelativeLayout no_gps_view;
    MTextView noLocTitleTxt, noLocMesageTxt, settingBtn, RetryBtn;
    InternetConnection intCheck;

    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_arrived);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        generalFunc = new GeneralFunctions(getActContext());

        AnimateMarker.driverMarkersPositionList.clear();
        AnimateMarker.driverMarkerAnimFinished = true;

        generalFunc.storedata(CommonUtilities.DRIVER_ONLINE_KEY, "false");
        isnotification = getIntent().getBooleanExtra("isnotification", false);

        //gps view declaration start

        no_gps_view = (RelativeLayout) findViewById(R.id.no_gps_view);

        noLocTitleTxt = (MTextView) findViewById(R.id.noLocTitleTxt);
        noLocMesageTxt = (MTextView) findViewById(R.id.noLocMesageTxt);
        settingBtn = (MTextView) findViewById(R.id.settingBtn);
        RetryBtn = (MTextView) findViewById(R.id.RetryBtn);

        settingBtn.setOnClickListener(new setOnClickList());
        RetryBtn.setOnClickListener(new setOnClickList());

        intCheck = new InternetConnection(getActContext());


        //gps view declaration end

        titleTxt = (MTextView) findViewById(R.id.titleTxt);
        addressTxt = (MTextView) findViewById(R.id.addressTxt);
        btn_type2 = ((MaterialRippleLayout) findViewById(R.id.btn_type2)).getChildView();
        map = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapV2);

        (findViewById(R.id.backImgView)).setVisibility(View.GONE);
        btn_type2.setId(Utils.generateViewId());

        emeTapImgView = (ImageView) findViewById(R.id.emeTapImgView);
        emeTapImgView.setOnClickListener(new setOnClickList());

        timeTxt = (MTextView) findViewById(R.id.timeTxt);

        //timeTxt = (MTextView) findViewById(timeTxt);


        setData();
//        if (isnotification) {
//            new OpenPassengerDetailDialog(getActContext(), data_trip, generalFunc, isnotification);
//        }
        if (generalFunc.retrieveValue("OPEN_CHAT").equals("Yes")) {
            generalFunc.storedata("OPEN_CHAT", "No");
            Bundle bnChat = new Bundle();

            bnChat.putString("iFromMemberId", data_trip.get("PassengerId"));
            bnChat.putString("FromMemberImageName", data_trip.get("PPicName"));
            bnChat.putString("iTripId", data_trip.get("iTripId"));
            bnChat.putString("FromMemberName", data_trip.get("PName"));

            new StartActProcess(getActContext()).startActWithData(ChatActivity.class, bnChat);
        }
        setLabels();

        generalFunc.storedata(CommonUtilities.DriverWaitingTime, "0");
        generalFunc.storedata(CommonUtilities.DriverWaitingSecTime, "0");

        tripMsgReceiver = new TripMessageReceiver((Activity) getActContext(), false);
        startLocationUpdateService = new Intent(getApplicationContext(), UpdateDriverLocationService.class);
        startLocationUpdateService.putExtra("PAppVersion", data_trip.get("PAppVersion"));

        map.getMapAsync(this);


        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) titleTxt.getLayoutParams();
        params.setMargins(Utils.dipToPixels(getActContext(), 20), 0, 0, 0);
        titleTxt.setLayoutParams(params);

        btn_type2.setOnClickListener(new setOnClickAct());


        registerTripMsgReceiver();

        startService(startLocationUpdateService);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            String restratValue_str = savedInstanceState.getString("RESTART_STATE");

            if (restratValue_str != null && !restratValue_str.equals("") && restratValue_str.trim().equals("true")) {
                generalFunc.restartApp();
                Utils.printLog("restartCall", "driverarrived11");
            }
        }

        if (generalFunc.isRTLmode()) {
            (findViewById(R.id.navStripImgView)).setRotation(180);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        handleNoLocationDial();
    }

    public boolean isPubNubEnabled() {
        String ENABLE_PUBNUB = generalFunc.retrieveValue(Utils.ENABLE_PUBNUB_KEY);

        return ENABLE_PUBNUB.equalsIgnoreCase("Yes");
    }

    public void setTimetext(String distance, String time) {
        try {
            String userProfileJson = generalFunc.retrieveValue(CommonUtilities.USER_PROFILE_JSON);
            String distance_str = "";
            if (userProfileJson != null && !generalFunc.getJsonValue("eUnit", userProfileJson).equalsIgnoreCase("KMs")) {
                timeTxt.setText(time + " " + generalFunc.retrieveLangLBl("to reach", "LBL_REACH_TXT") + " & " + distance + " " + generalFunc.retrieveLangLBl("", "LBL_MILE_DISTANCE_TXT") + " " + generalFunc.retrieveLangLBl("away", "LBL_AWAY_TXT"));
            } else {
                timeTxt.setText(time + " " + generalFunc.retrieveLangLBl("to reach", "LBL_REACH_TXT") + " & " + distance + " " + generalFunc.retrieveLangLBl("", "LBL_KM_DISTANCE_TXT") + " " + generalFunc.retrieveLangLBl("away", "LBL_AWAY_TXT"));

            }
        } catch (Exception e) {

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        outState.putString("RESTART_STATE", "true");
        super.onSaveInstanceState(outState);
    }

    public void handleNoLocationDial() {
        if (!generalFunc.isLocationEnabled()) {
            no_gps_view.setVisibility(View.VISIBLE);
            enableDisableViewGroup((RelativeLayout) findViewById(R.id.rootRelView), false);
            enableDisableViewGroup((RelativeLayout) findViewById(R.id.no_gps_view), true);
            return;
        }

        if (generalFunc.isLocationEnabled()) {
            no_gps_view.setVisibility(View.GONE);
            enableDisableViewGroup((RelativeLayout) findViewById(R.id.rootRelView), true);

            if (intCheck.isNetworkConnected() && intCheck.check_int() && addressTxt.getText().equals(generalFunc.retrieveLangLBl("Loading address", "LBL_LOAD_ADDRESS"))) {
                setData();
            }

            if (gMap == null && map != null && intCheck.isNetworkConnected() && intCheck.check_int())
                map.getMapAsync(this);
        }

    }

    public void setLabels() {

        // set Gps view lables start

        noLocTitleTxt.setText(generalFunc.retrieveLangLBl("", "LBL_ENABLE_LOC_SERVICE"));
        noLocMesageTxt.setText(generalFunc.retrieveLangLBl("This app requires location services. Please enabled location service from device settings. Go to Settings >> Location >>Turn on", "LBL_NO_LOCATION_ANDROID_TXT"));
        settingBtn.setText(generalFunc.retrieveLangLBl("Settings", "LBL_SETTINGS"));
        RetryBtn.setText(generalFunc.retrieveLangLBl("", "LBL_RETRY_TXT"));

        // set Gps view lables end

        setPageName();
        timeTxt.setText("--" + generalFunc.retrieveLangLBl("to reach", "LBL_REACH_TXT"));
        btn_type2.setText(generalFunc.retrieveLangLBl("", "LBL_BTN_ARRIVED_TXT"));
        ((MTextView) findViewById(R.id.navigateTxt)).setText(generalFunc.retrieveLangLBl("Navigate", "LBL_NAVIGATE"));
    }

    public void setPageName() {
        if (REQUEST_TYPE.equals("Deliver")) {
            titleTxt.setText(generalFunc.retrieveLangLBl("Pickup Delivery", "LBL_PICKUP_DELIVERY"));
        } else {
            titleTxt.setText(generalFunc.retrieveLangLBl("Pick Up Passenger", "LBL_PICK_UP_PASSENGER"));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.gMap = googleMap;
        if (generalFunc.checkLocationPermission(true) == true) {
            getMap().setMyLocationEnabled(false);
        }

        getMap().getUiSettings().setTiltGesturesEnabled(false);
        getMap().getUiSettings().setCompassEnabled(false);
        getMap().getUiSettings().setMyLocationButtonEnabled(false);

        getMap().setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.hideInfoWindow();
                return true;
            }
        });

        double passenger_lat = generalFunc.parseDoubleValue(0.0, data_trip.get("sourceLatitude"));
        double passenger_lon = generalFunc.parseDoubleValue(0.0, data_trip.get("sourceLongitude"));

        MarkerOptions marker_passenger_opt = new MarkerOptions()
                .position(new LatLng(passenger_lat, passenger_lon));


        int icon=R.drawable.taxi_passanger;
        if (REQUEST_TYPE.equals("Deliver"))
        {
            icon=R.drawable.taxi_passenger_delivery;
        }


        marker_passenger_opt.icon(BitmapDescriptorFactory.fromResource(icon)).anchor(0.5f,
                0.5f);
        getMap().addMarker(marker_passenger_opt);


        if (getLocationUpdates == null) {
            getLocationUpdates = new GetLocationUpdates(getActContext(), Utils.LOCATION_UPDATE_MIN_DISTANCE_IN_MITERS, true);
            getLocationUpdates.setLocationUpdatesListener(this);
        } else if (getLocationUpdates.getLocationUpdatesListener() == null) {
            getLocationUpdates.setLocationUpdatesListener(this);
        }

    }

    public GoogleMap getMap() {
        return this.gMap;
    }

    public void setData() {

        HashMap<String, String> data = (HashMap<String, String>) getIntent().getSerializableExtra("TRIP_DATA");

        this.data_trip = data;

        double passenger_lat = generalFunc.parseDoubleValue(0.0, data_trip.get("sourceLatitude"));
        double passenger_lon = generalFunc.parseDoubleValue(0.0, data_trip.get("sourceLongitude"));

        addressTxt.setText(generalFunc.retrieveLangLBl("Loading address", "LBL_LOAD_ADDRESS"));
        GetAddressFromLocation getAddressFromLocation = new GetAddressFromLocation(getActContext(), generalFunc);
        getAddressFromLocation.setLocation(passenger_lat, passenger_lon);
        getAddressFromLocation.setAddressList(new GetAddressFromLocation.AddressFound() {
            @Override
            public void onAddressFound(String address, double latitude, double longitude) {
                addressTxt.setText(address);
            }
        });
        getAddressFromLocation.execute();

        (findViewById(R.id.navigateArea)).setOnClickListener(new setOnClickAct("" + passenger_lat, "" + passenger_lon));

        REQUEST_TYPE = data_trip.get("REQUEST_TYPE");

        setPageName();
    }

    @Override
    public void onLocationUpdate(Location location) {

        if (location == null) {
            return;
        }

        if (location != null && this.userLocation == null) {
            this.userLocation = location;
            CameraPosition cameraPosition = cameraForUserPosition();
            getMap().moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        updateDriverMarker(new LatLng(location.getLatitude(), location.getLongitude()));

        this.userLocation = location;


        if (updateDirections == null) {
            Location destLoc = new Location("gps");
            destLoc.setLatitude(generalFunc.parseDoubleValue(0.0,data_trip.get("sourceLatitude")));
            destLoc.setLongitude(generalFunc.parseDoubleValue(0.0,data_trip.get("sourceLongitude")));
            updateDirections = new UpdateDirections(getActContext(), gMap, userLocation, destLoc);
            updateDirections.scheduleDirectionUpdate();

        } else if (updateDirections != null) {
            updateDirections.changeUserLocation(location);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Utils.REQUEST_CODE_GPS_ON) {
            // RequestForGpsAccess.activityResult(requestCode,resultCode,data);
           /* if (generalFunc.isLocationEnabled()) {
                no_gps_view.setVisibility(View.GONE);
                enableDisableViewGroup((RelativeLayout) findViewById(R.id.rootRelView), true);
            } else {

            }*/

            handleNoLocationDial();

        }
    }

    public void updateDriverMarker(LatLng newLocation) {
        if (driverMarker == null) {
            MarkerOptions markerOptions_driver = new MarkerOptions();
            markerOptions_driver.position(newLocation);

            int iconId = R.mipmap.car_driver;

            if (data_trip.containsKey("vVehicleType") )
            {
                if (data_trip.get("vVehicleType").equalsIgnoreCase("Bike")) {
                    iconId = R.mipmap.car_driver_1;
                } else if (data_trip.get("vVehicleType").equalsIgnoreCase("Cycle")) {
                    iconId = R.mipmap.car_driver_2;
                }
                else if (data_trip.get("vVehicleType").equalsIgnoreCase("Truck")) {
                    iconId = R.mipmap.car_driver_4;
                }

            }

            markerOptions_driver.icon(BitmapDescriptorFactory.fromResource(iconId)).anchor(0.5f,
                    0.5f).flat(true);

            driverMarker = gMap.addMarker(markerOptions_driver);
            //  driverMarker.setTitle(generalFunc.getMemberId());
        }

        if (this.userLocation != null && newLocation != null) {
            LatLng currentLatLng = new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude());
            float rotation = (float) SphericalUtil.computeHeading(currentLatLng, newLocation);


            if (driverMarker != null) {
                driverMarker.setTitle(generalFunc.getMemberId());
            }


            /**** Un used code****//*
            double rotationAngle = -1;

            if (assignedDriverRotatedLocation != null) {

                Location fromLoc = convertLntToLocation(assignedDriverRotatedLocation);
                Location toLoc = convertLntToLocation(driverLocation_update);

                double distanceInMeter = fromLoc.distanceTo(toLoc);
                Utils.printLog("Buffered", "RotationData:" + distanceInMeter + "::Latitude:From:" + fromLoc.getLatitude() + "::Longitude:From:" + fromLoc.getLongitude() + "::Latitude:To:" + toLoc.getLatitude() + "::Longitude:To:" + fromLoc.getLongitude());

                if (distanceInMeter > 1) {
                    rotationAngle = Utils.bearingBetweenLocations(assignedDriverRotatedLocation, driverLocation_update);
                } else {
                    rotationAngle = -1;
                }

                if (rotationAngle != -1) {
                    assignedDriverRotatedLocation = driverLocation_update;
                }
            } else {
                assignedDriverRotatedLocation = driverLocation_update;
            }

            if (rotationAngle > 180) {
                rotationAngle = rotationAngle - 360;
            } else {
                rotationAngle = 360 - rotationAngle;
            }
            Utils.printLog("rotationAngle", "::" + rotationAngle);*/

            HashMap<String, String> previousItemOfMarker = AnimateMarker.getLastLocationDataOfMarker(driverMarker);

            HashMap<String, String> data_map = new HashMap<>();
            data_map.put("vLatitude", "" + newLocation.latitude);
            data_map.put("vLongitude", "" + newLocation.longitude);
            data_map.put("iDriverId", "" + generalFunc.getMemberId());
            data_map.put("RotationAngle", "" + rotation);
            data_map.put("LocTime", "" + System.currentTimeMillis());


            if (previousItemOfMarker.get("LocTime") != null && !previousItemOfMarker.get("LocTime").equals("")) {

                long previousLocTime = generalFunc.parseLongValue(0, previousItemOfMarker.get("LocTime"));
                long newLocTime = generalFunc.parseLongValue(0, data_map.get("LocTime"));

                if (previousLocTime != 0 && newLocTime != 0) {

                    if ((newLocTime - previousLocTime) > 0 && AnimateMarker.driverMarkerAnimFinished == false) {
                        AnimateMarker.driverMarkersPositionList.add(data_map);
                    } else if ((newLocTime - previousLocTime) > 0) {
                        AnimateMarker.animateMarker(driverMarker, this.gMap, newLocation, rotation, 800, tripId, data_map.get("LocTime"));
                    }

                } else if ((previousLocTime == 0 || newLocTime == 0) && AnimateMarker.driverMarkerAnimFinished == false) {
                    AnimateMarker.driverMarkersPositionList.add(data_map);
                } else {
                    AnimateMarker.animateMarker(driverMarker, this.gMap, newLocation, rotation, 800, tripId, data_map.get("LocTime"));
                }
            } else if (AnimateMarker.driverMarkerAnimFinished == false) {
                AnimateMarker.driverMarkersPositionList.add(data_map);
            } else {
                AnimateMarker.animateMarker(driverMarker, this.gMap, newLocation, rotation, 1200, tripId, data_map.get("LocTime"));
            }
        }
    }

    public CameraPosition cameraForUserPosition() {
        double currentZoomLevel = getMap().getCameraPosition().zoom;

        if (Utils.defaultZomLevel > currentZoomLevel) {
            currentZoomLevel = Utils.defaultZomLevel;
        }
        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude()))
                .zoom((float) currentZoomLevel).build();

        return cameraPosition;
    }

    public void tripCancelled() {
        final GenerateAlertBox generateAlert = new GenerateAlertBox(getActContext());
        generateAlert.setCancelable(false);
        generateAlert.setBtnClickList(new GenerateAlertBox.HandleAlertBtnClick() {
            @Override
            public void handleBtnClick(int btn_id) {
                generateAlert.closeAlertBox();
                generalFunc.saveGoOnlineInfo();
                // generalFunc.restartApp();
                generalFunc.restartwithGetDataApp();
            }
        });
        generateAlert.setContentMessage("", generalFunc.retrieveLangLBl("", "LBL_PASSENGER_CANCEL_TRIP_TXT"));
        generateAlert.setPositiveBtn(generalFunc.retrieveLangLBl("", "LBL_BTN_OK_TXT"));
        generateAlert.showAlertBox();
    }

    public void buildMsgOnArrivedBtn() {
        final GenerateAlertBox generateAlert = new GenerateAlertBox(getActContext());
        generateAlert.setCancelable(false);
        generateAlert.setBtnClickList(new GenerateAlertBox.HandleAlertBtnClick() {
            @Override
            public void handleBtnClick(int btn_id) {
                if (btn_id == 0) {
                    btn_type2.setEnabled(true);
                    generateAlert.closeAlertBox();
                } else {
                    btn_type2.setEnabled(true);
                    setDriverStatusToArrived();
                }

            }
        });
        generateAlert.setContentMessage("", generalFunc.retrieveLangLBl("", "LBL_ARRIVED_CONFIRM_DIALOG_TXT"));
        generateAlert.setPositiveBtn(generalFunc.retrieveLangLBl("", "LBL_BTN_OK_TXT"));
        generateAlert.setNegativeBtn(generalFunc.retrieveLangLBl("", "LBL_CANCEL_TXT"));

        generateAlert.showAlertBox();

        // generateAlert.getAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1C1C1C"));
        // generateAlert.getAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#909090"));
    }

    public void setDriverStatusToArrived() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("type", "DriverArrived");
        parameters.put("iDriverId", generalFunc.getMemberId());

        ExecuteWebServerUrl exeWebServer = new ExecuteWebServerUrl(getActContext(), parameters);
        exeWebServer.setLoaderConfig(getActContext(), true, generalFunc);
        exeWebServer.setDataResponseListener(new ExecuteWebServerUrl.SetDataResponse() {
            @Override
            public void setResponse(String responseString) {

                if (responseString != null && !responseString.equals("")) {

                    boolean isDataAvail = GeneralFunctions.checkDataAvail(CommonUtilities.action_str, responseString);

                    if (isDataAvail == true) {
                        unRegisterReceiver();
                        stopDriverLocationUpdateService();

                        String message = generalFunc.getJsonValue(CommonUtilities.message_str, responseString);

                        data_trip.put("DestLocLatitude", generalFunc.getJsonValue("DLatitude", message));
                        data_trip.put("DestLocLongitude", generalFunc.getJsonValue("DLongitude", message));
                        data_trip.put("DestLocAddress", generalFunc.getJsonValue("DAddress", message));
                        data_trip.put("eTollSkipped", generalFunc.getJsonValue("eTollSkipped", message));
                        data_trip.put("vTripStatus", "Arrived");


                        if (updateDirections != null) {
                            updateDirections.releaseTask();
                            updateDirections = null;
                        }

                        stopPubNub();

//                        Bundle bn = new Bundle();
//                        bn.putSerializable("TRIP_DATA", data_trip);
//
//
//                        new StartActProcess(getActContext()).startActWithData(ActiveTripActivity.class, bn);

                        generalFunc.restartwithGetDataApp();

//                        stopPubNub();
                        //   ActivityCompat.finishAffinity(DriverArrivedActivity.this);

                    } else {
                        String msg_str = generalFunc.getJsonValue(CommonUtilities.message_str, responseString);
                        if (msg_str.equals("DO_RESTART") ||
                                msg_str.equals(CommonUtilities.GCM_FAILED_KEY) || msg_str.equals(CommonUtilities.APNS_FAILED_KEY) || msg_str.equals("LBL_SERVER_COMM_ERROR")) {
                            generalFunc.restartApp();
                            Utils.printLog("restartCall", "driverarrived==555");
                        } else {
                            generalFunc.showGeneralMessage("",
                                    generalFunc.retrieveLangLBl("", generalFunc.getJsonValue(CommonUtilities.message_str, responseString)));
                        }

                    }
                } else {
                    generalFunc.showError();
                }
            }
        });
        exeWebServer.execute();
    }

    public boolean onCreateOptionsMenu(Menu menu) {


        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.trip_accept_menu, menu);

        if (REQUEST_TYPE.equals("Deliver")) {
            menu.findItem(R.id.menu_passenger_detail).setTitle(generalFunc.retrieveLangLBl("View Delivery Details", "LBL_VIEW_DELIVERY_DETAILS"));
            menu.findItem(R.id.menu_cancel_trip).setTitle(generalFunc.retrieveLangLBl("Cancel Delivery", "LBL_CANCEL_DELIVERY"));
        } else {
            menu.findItem(R.id.menu_passenger_detail).setTitle(generalFunc.retrieveLangLBl("View passenger detail", "LBL_VIEW_PASSENGER_DETAIL"));
            menu.findItem(R.id.menu_cancel_trip).setTitle(generalFunc.retrieveLangLBl("Cancel trip", "LBL_CANCEL_TRIP"));
        }


        if (!REQUEST_TYPE.equals(Utils.CabGeneralType_UberX)) {
            menu.findItem(R.id.menu_waybill_trip).setTitle(generalFunc.retrieveLangLBl("Way Bill", "LBL_MENU_WAY_BILL")).setVisible(true);

        } else {
            menu.findItem(R.id.menu_waybill_trip).setTitle(generalFunc.retrieveLangLBl("Way Bill", "LBL_MENU_WAY_BILL")).setVisible(false);


        }

        menu.findItem(R.id.menu_call).setTitle(generalFunc.retrieveLangLBl("Call", "LBL_CALL_ACTIVE_TRIP"));
        menu.findItem(R.id.menu_message).setTitle(generalFunc.retrieveLangLBl("Message", "LBL_MESSAGE_ACTIVE_TRIP"));
        menu.findItem(R.id.menu_sos).setTitle(generalFunc.retrieveLangLBl("Emergency or SOS", "LBL_EMERGENCY_SOS_TXT")).setVisible(false);

        if (REQUEST_TYPE.equals(Utils.CabGeneralType_UberX) &&
                !data_trip.get("eFareType").equalsIgnoreCase(Utils.CabFaretypeRegular)) {
            menu.findItem(R.id.menu_passenger_detail).setVisible(false);
            menu.findItem(R.id.menu_call).setVisible(true);
            menu.findItem(R.id.menu_message).setVisible(true);

        } else {
            menu.findItem(R.id.menu_passenger_detail).setVisible(true);
            menu.findItem(R.id.menu_call).setVisible(false);
            menu.findItem(R.id.menu_message).setVisible(false);

        }

        Utils.setMenuTextColor(menu.findItem(R.id.menu_sos), getResources().getColor(R.color.appThemeColor_TXT_1));
        Utils.setMenuTextColor(menu.findItem(R.id.menu_call), getResources().getColor(R.color.appThemeColor_TXT_1));
        Utils.setMenuTextColor(menu.findItem(R.id.menu_message), getResources().getColor(R.color.appThemeColor_TXT_1));
        Utils.setMenuTextColor(menu.findItem(R.id.menu_passenger_detail), getResources().getColor(R.color.appThemeColor_TXT_1));
        Utils.setMenuTextColor(menu.findItem(R.id.menu_cancel_trip), getResources().getColor(R.color.appThemeColor_TXT_1));
        Utils.setMenuTextColor(menu.findItem(R.id.menu_waybill_trip), getResources().getColor(R.color.appThemeColor_TXT_1));
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {

            // perform your desired action here

            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_passenger_detail:


                if (REQUEST_TYPE.equals("Deliver")) {
                    Bundle bn = new Bundle();
                    bn.putString("TripId", data_trip.get("TripId"));
                    bn.putSerializable("data_trip", data_trip);




                } else {
                    new OpenPassengerDetailDialog(getActContext(), data_trip, generalFunc, false);
                }

                return true;


            case R.id.menu_cancel_trip:
                new CancelTripDialog(getActContext(), data_trip, generalFunc, false);
                return true;

            case R.id.menu_waybill_trip:
               // new StartActProcess(getActContext()).startAct(WayBillActivity.class);
                Bundle bn=new Bundle();
                bn.putSerializable("data_trip", data_trip);
                new StartActProcess(getActContext()).startActWithData(WayBillActivity.class,bn);
                return true;

            case R.id.menu_call:
                if (REQUEST_TYPE.equals("Deliver")) {
                    Bundle bn1 = new Bundle();
                    bn1.putString("TripId", data_trip.get("TripId"));
                    bn1.putSerializable("data_trip", data_trip);

                } else {
                    try {

                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        // callIntent.setData(Uri.parse("tel:" + data_trip.get("PPhoneC") + "" + data_trip.get("PPhone")));
                        String phoneCode = data_trip.get("PPhoneC") != null && Utils.checkText(data_trip.get("PPhoneC")) ? "+" + data_trip.get("PPhoneC") : "";
                        callIntent.setData(Uri.parse("tel:" + data_trip.get("PPhone")));
                        getActContext().startActivity(callIntent);

                    } catch (Exception e) {
                    }
                }


                return true;
            case R.id.menu_message:

                if (REQUEST_TYPE.equals("Deliver")) {
                    Bundle bn2 = new Bundle();
                    bn2.putString("TripId", data_trip.get("TripId"));
                    bn2.putSerializable("data_trip", data_trip);

                }
            {

                Bundle bnChat = new Bundle();

                bnChat.putString("iFromMemberId", data_trip.get("PassengerId"));
                bnChat.putString("FromMemberImageName", data_trip.get("PPicName"));
                bnChat.putString("iTripId", data_trip.get("iTripId"));
                bnChat.putString("FromMemberName", data_trip.get("PName"));

                new StartActProcess(getActContext()).startActWithData(ChatActivity.class, bnChat);
            }
            return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void registerTripMsgReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CommonUtilities.passenger_message_arrived_intent_action_trip_msg);

        registerReceiver(tripMsgReceiver, filter);

        if (isPubNubEnabled()) {
            configPubNub = new ConfigPubNub(getActContext());
            configPubNub.setTripId(data_trip.get("iTripId"), data_trip.get("PassengerId"));
        }
    }

    public void unRegisterReceiver() {
        if (tripMsgReceiver != null) {
            try {
                unregisterReceiver(tripMsgReceiver);
            } catch (Exception e) {

            }
        }
    }

    public void stopDriverLocationUpdateService() {
        try {
            stopService(startLocationUpdateService);
        } catch (Exception e) {

        }
    }

    public void stopPubNub() {
        if (configPubNub != null) {
            configPubNub.unSubscribeToPrivateChannel();
            configPubNub.releaseInstances();
            configPubNub = null;
            Utils.runGC();
        }
    }

    @Override
    protected void onDestroy() {
        unRegisterReceiver();
        stopDriverLocationUpdateService();
        stopPubNub();
        if (updateDirections != null) {
            updateDirections.releaseTask();
            updateDirections = null;
        }
        super.onDestroy();
    }

    public Context getActContext() {
        return DriverArrivedActivity.this; // Must be context of activity not application
    }

    public void openNavigationDialog(final String passenger_lat, final String passenger_lon) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActContext());

        LayoutInflater inflater = (LayoutInflater) getActContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_selectnavigation_view, null);

        final MTextView NavigationTitleTxt = (MTextView) dialogView.findViewById(R.id.NavigationTitleTxt);
        final MTextView wazemapTxtView = (MTextView) dialogView.findViewById(R.id.wazemapTxtView);
        final MTextView googlemmapTxtView = (MTextView) dialogView.findViewById(R.id.googlemmapTxtView);
        final RadioButton radiogmap = (RadioButton) dialogView.findViewById(R.id.radiogmap);
        final RadioButton radiowazemap = (RadioButton) dialogView.findViewById(R.id.radiowazemap);

        radiogmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radiogmap.setChecked(true);
                radiowazemap.setChecked(false);
                googlemmapTxtView.performClick();

            }
        });
        radiowazemap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radiogmap.setChecked(false);
                radiowazemap.setChecked(true);
                wazemapTxtView.performClick();

            }
        });

        builder.setView(dialogView);
        NavigationTitleTxt.setText(generalFunc.retrieveLangLBl("Choose Option", "LBL_CHOOSE_OPTION"));
        googlemmapTxtView.setText(generalFunc.retrieveLangLBl("Google map navigation", "LBL_NAVIGATION_GOOGLE_MAP"));
        wazemapTxtView.setText(generalFunc.retrieveLangLBl("Waze navigation", "LBL_NAVIGATION_WAZE"));


        googlemmapTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Utils.printLog("passenger_lat", passenger_lat + "");
                    Utils.printLog("passenger_lon", passenger_lon + "");
                    String url_view = "http://maps.google.com/maps?daddr=" + passenger_lat + "," + passenger_lon;
                    (new StartActProcess(getActContext())).openURL(url_view, "com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                    list_navigation.dismiss();
                } catch (Exception e) {
                    generalFunc.showMessage(wazemapTxtView, generalFunc.retrieveLangLBl("Please install Google Maps in your device.", "LBL_INSTALL_GOOGLE_MAPS"));
                }

            }
        });

        wazemapTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    String uri = "waze://?ll=" + passenger_lat + "," + passenger_lon + "&navigate=yes";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                    list_navigation.dismiss();
                } catch (Exception e) {

                    generalFunc.showMessage(wazemapTxtView, generalFunc.retrieveLangLBl("Please install Waze navigation app in your device.", "LBL_INSTALL_WAZE"));


                }


            }
        });


        list_navigation = builder.create();
        if (generalFunc.isRTLmode() == true) {
            generalFunc.forceRTLIfSupported(list_navigation);
        }
        list_navigation.show();
        list_navigation.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Utils.hideKeyboard(getActContext());
            }
        });
    }

    public class setOnClickList implements View.OnClickListener {

        @Override
        public void onClick(View view) {


            if (view.getId() == emeTapImgView.getId()) {
                Bundle bn = new Bundle();

                bn.putString("TripId", tripId);
                new StartActProcess(getActContext()).startActWithData(ConfirmEmergencyTapActivity.class, bn);
            } else if (view.getId() == settingBtn.getId()) {
                new StartActProcess(getActContext()).
                        startActForResult(Settings.ACTION_LOCATION_SOURCE_SETTINGS, Utils.REQUEST_CODE_GPS_ON);
            } else if (view.getId() == RetryBtn.getId()) {
                if (generalFunc.isLocationEnabled()) {
                    no_gps_view.setVisibility(View.GONE);
                    enableDisableViewGroup((RelativeLayout) findViewById(R.id.rootRelView), true);
                }
            }

        }
    }

    public class setOnClickAct implements View.OnClickListener {

        String passenger_lat = "";
        String passenger_lon = "";

        public setOnClickAct() {
        }

        public setOnClickAct(String passenger_lat, String passenger_lon) {
            this.passenger_lat = passenger_lat;
            this.passenger_lon = passenger_lon;
        }

        @Override
        public void onClick(View view) {
            int i = view.getId();
            Utils.hideKeyboard(DriverArrivedActivity.this);
            if (i == btn_type2.getId()) {
                btn_type2.setEnabled(false);
                buildMsgOnArrivedBtn();
            } else if (i == R.id.navigateArea) {
                openNavigationDialog(passenger_lat, passenger_lon);
            }
        }
    }

}
