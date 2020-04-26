package com.sai.intelligent.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.sai.intelligent.services.UploadFilesService;
import com.sai.intelligent.utils.Commons;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class PhoneCallReciever extends BroadcastReceiver {


    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;
    private static MediaRecorder mediaRecorder;
    private File recordedFile;
    private Context context;
    private static final String TAG = "PhoneCallReciever";

    public PhoneCallReciever() {
        Log.d(TAG, "Phone call reciever object created...!");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");

        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }

            onCallStateChanged(context, state, number);
        }
    }


    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                //startCallRecording();
                //Toast.makeText(context, "Incoming Call Ringing" , Toast.LENGTH_SHORT).show();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    startCallRecording();
                    //Toast.makeText(context, "Outgoing Call Started" , Toast.LENGTH_SHORT).show();
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    //Toast.makeText(context, "Ringing but no pickup" + savedNumber + " Call time " + callStartTime +" Date " + new Date() , Toast.LENGTH_SHORT).show();
                } else if (isIncoming) {
                    stopCallRecording();
//                    Toast.makeText(context, "Incoming " + savedNumber + " Call time " + callStartTime  , Toast.LENGTH_SHORT).show();
                } else {
                    stopCallRecording();
//                    Toast.makeText(context, "outgoing " + savedNumber + " Call time " + callStartTime +" Date " + new Date() , Toast.LENGTH_SHORT).show();

                }

                break;
        }
        lastState = state;
    }

    private void startCallRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        File file = new File(context.getFilesDir().getPath(), "RecordedCalls");
        if (!file.exists()) file.mkdir();
        recordedFile = new File(file.getAbsolutePath(), Commons.getCurrentTimeStamp() + ".3gp");
        mediaRecorder.setOutputFile(recordedFile.getAbsolutePath());
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Call recording started for " + savedNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCallRecording() {
        mediaRecorder.stop();
        if (Commons.isInternetAvailable(context) == true) {
            Intent intent = new Intent(context, UploadFilesService.class);
            intent.putExtra("folderName", "RecordedCalls");
            context.startService(intent);
        }
        Log.d(TAG, "Call recording stopped");
        mediaRecorder.release();
    }
}
