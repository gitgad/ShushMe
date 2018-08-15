package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class Geofencing implements ResultCallback{

    // Constants
    public static final String TAG = Geofencing.class.getSimpleName();
    private static final float GEOFENCE_RADIUS = 50; // 50 meters
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours

    private Context mContext;
    private GoogleApiClient mClient;
    private List<Geofence> mGeofencesList;
    private PendingIntent mGeofencePendingIntent;

    public Geofencing(Context context, GoogleApiClient client){
        mContext = context;
        mClient = client;
        mGeofencePendingIntent = null;
        mGeofencesList = new ArrayList<>();
    }

    public void registerAllGeofences(){
        //Check that the API client is connected and that the list has Geofences in it
        if(mClient == null || !mClient.isConnected() ||
                mGeofencesList == null || mGeofencesList.size() == 0){
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e(TAG, securityException.getMessage());
        }
    }

    public void unregisterAllGeofences(){
        //Check that the API client is connected and that the list has Geofences in it
        if(mClient == null || !mClient.isConnected()){
            return;
        }

        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mClient,
                    // Same pending intent that was used in registerGeofences
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e(TAG, securityException.getMessage());
        }
    }

    public void updateGeofencesList(PlaceBuffer places){
        mGeofencesList = new ArrayList<>();

        if(places == null || places.getCount() == 0){
            return;
        }

        for(Place place : places){
            // Read the place information from the DB cursor
            String placeUID = place.getId();
            double placeLat = place.getLatLng().latitude;
            double placeLong = place.getLatLng().longitude;

            // Build a geofence object
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeUID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLong, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            // Add geofence to list
            mGeofencesList.add(geofence);
        }
    }

    private GeofencingRequest getGeofencingRequest(){
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER);
        builder.addGeofences(mGeofencesList);

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(){
        // Reuse the pending intent if we all ready have it
        if(mGeofencePendingIntent != null){
            return mGeofencePendingIntent;
        }

        Intent broadcastIntent = new Intent(mContext, GeofenceBroadcastReciever.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0,
                broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG, String.format("Error adding/removing geofence : %s",
                result.getStatus().toString()));
    }
}
