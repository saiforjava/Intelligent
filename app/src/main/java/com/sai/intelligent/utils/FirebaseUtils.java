package com.sai.intelligent.utils;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FirebaseUtils {

    public static final String TAG = "FirebaseUtils";

    public static void saveFCMToken(String mobileno,String fcmToken) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Map<String,Object> map = new HashMap<>();
        map.put(mobileno,fcmToken);
        databaseReference.child("/device").setValue(map).addOnSuccessListener(aVoid -> Log.d(TAG,"Fcm token saved to firebase datastore"));
    }

    public static void uploadFile(String folderName,String givenFile) {
        File recordedfile = new File(givenFile);
        Uri file = Uri.fromFile(recordedfile);
        StorageReference surroundRecordRef = FirebaseStorage.getInstance().getReference().child(folderName+"/"+file.getLastPathSegment());
        UploadTask uploadTask = surroundRecordRef.putFile(file);
        uploadTask.addOnSuccessListener(command -> {
            Log.d(TAG, "File uploaded to firebase store");
            recordedfile.delete();
        }).addOnFailureListener(e -> Log.d(TAG, "File upload failed to firebase store and exception is : " + e.getMessage()));
    }
}
