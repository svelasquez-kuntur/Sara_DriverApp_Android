package com.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.general.files.GeneralFunctions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Admin on 28-09-2017.
 */
public class AnimateMarker {

    public static boolean driverMarkerAnimFinished = true;

    public static ArrayList<HashMap<String, String>> driverMarkersPositionList = new ArrayList<>();

    static boolean isRotating = false;

    /*public static void rotateMarker(final Marker marker, final float toRotation, final long duration) {

        if (!isRotating) {
            isRotating = true;
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
//            final long duration = 1000;
            float deltaRotation = Math.abs(toRotation - startRotation) % 360;
            final float rotation = (deltaRotation > 180 ? 360 - deltaRotation : deltaRotation) *
                    ((toRotation - startRotation >= 0 && toRotation - startRotation <= 180) || (toRotation - startRotation <= -180 && toRotation - startRotation >= -360) ? 1 : -1);

            final LinearInterpolator interpolator = new LinearInterpolator();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);
                    marker.setRotation((startRotation + t * rotation) % 360);
                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        isRotating = false;
                    }
                }
            });
        }

    }*/

    public static HashMap<String, String> getNextBufferedLocationData(String iDriverId) {

        for (int i = 0; i < driverMarkersPositionList.size(); i++) {
            HashMap<String, String> item = driverMarkersPositionList.get(i);
            if (item.get("iDriverId").equals(iDriverId)) {
                item.put("Position", "" + i);
                return item;
            }
        }
        return null;
    }

    public static void removeBufferedLocation(String iDriverId, String LocTime) {

        for (int i = 0; i < driverMarkersPositionList.size(); i++) {
            HashMap<String, String> item = driverMarkersPositionList.get(i);
            if (item.get("LocTime").equals(LocTime) && item.get("iDriverId").equals(iDriverId)) {
                driverMarkersPositionList.remove(i);
                break;
            }
        }

    }

    public static void animateMarker(final Marker marker, final GoogleMap gMap, final LatLng toPosition, final float rotationAngle, final float duration, final String iDriverId, final String locTime) {
        if (marker == null || toPosition == null || gMap == null) {
            return;
        }

        AnimateMarker.driverMarkerAnimFinished = false;

        int gMapDuration = (int) (duration < 0 ? 2 : duration);

        try {

            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(toPosition)
                    .zoom(gMap.getCameraPosition().zoom).build()), gMapDuration, null);
        } catch (Exception e) {
        }
//		rotateMarker(marker, toPosition, gMap, (float) rotationAngle, duration, iDriverId, locTime);

//        if (rotationAngle != -1) {
//            rotateMarker(marker, (float) rotationAngle, (long) duration);
//        }

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final float startRotation = marker.getRotation();
//            final long duration = 1000;
        float deltaRotation = Math.abs(rotationAngle - startRotation) % 360;
        final float rotation = (deltaRotation > 180 ? 360 - deltaRotation : deltaRotation) *
                ((rotationAngle - startRotation >= 0 && rotationAngle - startRotation <= 180) || (rotationAngle - startRotation <= -180 && rotationAngle - startRotation >= -360) ? 1 : -1);

        Projection proj = gMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final LinearInterpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (!isRotating) {
                    marker.setRotation((startRotation + t * rotation) % 360);
                }
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);

                } else {
                    isRotating = false;

                    AnimateMarker.driverMarkerAnimFinished = true;

                    AnimateMarker.removeBufferedLocation(iDriverId, locTime);

                    HashMap<String, String> data_temp = getNextBufferedLocationData(iDriverId);
                    if (data_temp != null) {

                        LatLng location_temp = new LatLng(GeneralFunctions.parseDoubleValue(0.0, data_temp.get("vLatitude")), GeneralFunctions.parseDoubleValue(0.0, data_temp.get("vLongitude")));
                        float newDuration = driverMarkersPositionList.size() > 0 ? (duration / (driverMarkersPositionList.size() * 4)) : duration;
                        AnimateMarker.animateMarker(marker, gMap, location_temp, GeneralFunctions.parseFloatValue(-1, data_temp.get("RotationAngle")), newDuration, iDriverId, data_temp.get("LocTime"));
                    }
                    marker.setVisible(true);

                }


            }
        });
//

    }


    public static HashMap<String, String> getLastLocationDataOfMarker(Marker marker) {

        if (marker == null || marker.getTitle() == null || marker.getTitle() == "") {
            return (new HashMap<String, String>());
        }

        int lastIndex = driverMarkersPositionList.size() - 1;

        for (int i = 0; i < driverMarkersPositionList.size(); i++) {
            HashMap<String, String> item = driverMarkersPositionList.get(lastIndex - i);

            if (item.get("iDriverId").equals(marker.getTitle().replace("DriverId", ""))) {
                return item;
            }
        }

        return (new HashMap<String, String>());
    }

    /* Non Use Method */
    public static double bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double f1 = Math.PI * latLng1.latitude / 180;
        double f2 = Math.PI * latLng2.latitude / 180;
        double dl = Math.PI * (latLng2.longitude - latLng1.longitude) / 180;
        return Math.atan2(Math.sin(dl) * Math.cos(f2), Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl));
    }

}
