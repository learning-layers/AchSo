package fi.aalto.legroup.achso.util;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fi.aalto.legroup.achso.state.i5LoginState;

public class App extends Application {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static i5LoginState login_state;
    private static Context mContext;
    private static File mLogFile;
    public static Location last_location;
    public String last_qr_code;
    public Date last_qr_code_timestamp;


    public static Context getContext() {
        return mContext;
    }

    public static boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static void appendLog(String text) {
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(mLogFile, true));
            buf.append(String.format("%s %s", sdf.format(new Date()), text));
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mLogFile = new File(mContext.getExternalFilesDir(null), "achso.log");
        if (!mLogFile.exists()) {
            try {
                boolean ok = mLogFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        login_state = new i5LoginState(mContext);
        appendLog("Starting Ach so! -app on device " + android.os.Build.MODEL);
        Log.i("App", "Starting Ach so! -app on device " + android.os.Build.MODEL);

    }

    public static void getLocation() {
        // NOTE: There's a small possibility that the location could not be retrieved before the
        // video recording is finished. Is this acceptable or is there a need for a waiting dialog?

        final LocationManager locationManager = (LocationManager) mContext.getSystemService(Context
                .LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                last_location = location;
                //Log.i("LocationListener", "Found location: " + location.toString());
                Toast.makeText(mContext, "Found location: " + location.toString(), Toast.LENGTH_LONG).show();
                // take location only once
                locationManager.removeUpdates(this);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

    }


}


