package com.sai.intelligent.services;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sai.intelligent.utils.Commons;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class SurroundRecorderService extends Service implements MediaRecorder.OnErrorListener {

    private static final String TAG = "SurroundRecorderService";
    private File newFile;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSurroundRecording(intent.getIntExtra("time", 10));
        return Service.START_NOT_STICKY;
    }



    private void startSurroundRecording(int recordTime) {


        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mediaRecorder.setMaxDuration(recordTime);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setOnErrorListener(this);
        File file = new File(getApplicationContext().getFilesDir().getPath(), "SurroundAudios");
        if (!file.exists()) file.mkdir();
        try {
            newFile = new File(file.getAbsolutePath(), Commons.getCurrentTimeStamp() + ".3gp");

//            if (newFile.exists() != true) newFile.createNewFile();
            mediaRecorder.setOutputFile(newFile.getAbsolutePath());

            Log.d(TAG, "File location: " + newFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    timer.cancel();
                    Log.d(TAG, "Recording completed........!");
                    if (Commons.isInternetAvailable(getApplicationContext()) == true) {
                        Intent intent = new Intent(SurroundRecorderService.this, UploadFilesService.class);
                        intent.putExtra("folderName", "SurroundAudios");
                        startService(intent);
                    }
                }
            }, recordTime + 10);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }






//    @Override
//    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
//
//            Log.d(TAG,"Maximum Duration Reached "+what);
//
//    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
        Log.d(TAG,"Error in recording...!");
    }





}
