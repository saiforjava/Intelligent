package com.sai.intelligent.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Commons {

    public static String getCurrentTimeStamp(){
        SimpleDateFormat s = new SimpleDateFormat("dd-MMM-yyyy:hh-mm-ss");
        return s.format(new Date());
    }
}
