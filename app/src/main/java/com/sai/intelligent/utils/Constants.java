package com.sai.intelligent.utils;

import android.app.Activity;

public class Constants {

    /** Result code returned when there is a generic client error */
    public static final int RESULT_CLIENT_ERROR = Activity.RESULT_FIRST_USER + 1;
    /** Result code returned when an audio error was encountered */
    public static final int RESULT_AUDIO_ERROR = Activity.RESULT_FIRST_USER + 4;

    /* The maximum recording time must be smaller than the max recording time specified
     * in RawAudioRecorder if we want to avoid buffer overflow*/
    public static final int MAX_RECORDING_TIME = 10;
    public static final int MAX_RECORDING_TIME_TOKEN = 15;
    public static final int MAX_RECORDING_TIME_FREE_SPEECH = 29;

}
