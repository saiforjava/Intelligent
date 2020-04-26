package com.sai.intelligent.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Commons {

    public static String getCurrentTimeStamp(){
        SimpleDateFormat s = new SimpleDateFormat("dd-MMM-yyyy:hh-mm-ss");
        return s.format(new Date());
    }

    public static boolean isInternetAvailable(Context context) {
        boolean status = false;
        ConnectivityManager connectivityMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (connectivityMgr != null) {
            activeNetwork = connectivityMgr.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    NetworkCapabilities nc = connectivityMgr.getNetworkCapabilities(connectivityMgr.getActiveNetwork());
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        status = true;
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        status = true;
                    }
                } else {

                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        status = true;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        status = true;
                    }

                }
            }
        }
        return status;
    }
}
