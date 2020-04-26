package com.sai.intelligent.services;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.sai.intelligent.utils.FirebaseUtils;

import java.io.File;


public class UploadFilesService extends Service {


    private static final String TAG = "UploadFilesService";


    private void uploadFiles(String folderName) {

        File file = new File(getApplicationContext().getFilesDir().getPath(), folderName);
        search(".*\\.3gp", file, folderName);

    }

    public static void search(final String pattern, final File folder, String folderName) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                search(pattern, f, folderName);
            }

            if (f.isFile()) {
                if (f.getName().matches(pattern)) {
                    FirebaseUtils.uploadFile(folderName, f.getPath());
                    //f.delete();
                }
            }

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uploadFiles(intent.getStringExtra("folderName"));
        return START_STICKY;
    }
}
