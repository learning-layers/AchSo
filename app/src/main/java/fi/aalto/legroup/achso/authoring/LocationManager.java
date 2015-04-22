package fi.aalto.legroup.achso.authoring;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Manages listening for location updates and returning the last found location.
 */
public final class LocationManager implements GoogleApiClient.ConnectionCallbacks,
        LocationListener {

    private Location lastLocation;
    private GoogleApiClient locationApiClient;

    public LocationManager(Context context) {
        locationApiClient = new GoogleApiClient.Builder(context)
                .useDefaultAccount()
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
    }

    /**
     * Location updates should be requested when recording starts. This should give us enough time
     * to fetch an accurate location.
     */
    public void startLocationUpdates() {
        locationApiClient.connect();
    }

    /**
     * When recording has finished, ask for the location via this method, so we'll stop listening
     * for further location updates.
     */
    public Location getLastLocation() {
        if (locationApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationApiClient, this);
            locationApiClient.disconnect();
        }

        return lastLocation;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(locationApiClient,
                locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

}
