package com.sai.intelligent.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.sai.intelligent.services.UploadFilesService;

public class InternetConnectionStateReciever extends BroadcastReceiver {

    private final static String TAG = "InternetReceiver";

    public InternetConnectionStateReciever() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager
                    .getActiveNetworkInfo();

            // Check internet connection and accrding to state change the
            // text of activity by calling method
            if (networkInfo != null && networkInfo.isConnected()) {
                Log.d(TAG, "Internet connection detected...!");
                context.startService(new Intent(context, UploadFilesService.class));
            } else {
                Log.d(TAG, "Network away again........!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}