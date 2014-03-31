package fi.aalto.legroup.achso.util;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fi.aalto.legroup.achso.state.LoginState;

public class App extends Application {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static LoginState login_state;
    private static Context mContext;
    private static File mLogFile;

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

        login_state = new LoginState(mContext);
        appendLog("Starting Ach so! -app on device " + android.os.Build.MODEL);
        Log.i("App", "Starting Ach so! -app on device " + android.os.Build.MODEL);

    }

}


