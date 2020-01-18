package com.sai.intelligent.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.os.Build;
import android.util.Log;

public class RawAudioRecorder {

    private static final String LOG_TAG = RawAudioRecorder.class.getName();

    public static final int MAX_RECORDING_TIME_IN_SECS = 30 /*seconds*/;

    //	private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
//	private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
//	private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int DEFAULT_SAMPLE_RATE = 8000;

    private static final int RESOLUTION = AudioFormat.ENCODING_PCM_16BIT;
    private static final short RESOLUTION_IN_BYTES = 2;

    // Number of channels (MONO = 1, STEREO = 2)
    private static final short CHANNELS = 1;

    public enum State {
        // recorder is ready, but not yet recording
        READY,

        // recorder recording
        RECORDING,

        // error occurred, reconstruction needed
        ERROR,

        // recorder stopped
        STOPPED
    };

    private AudioRecord mRecorder = null;

    private double mAvgEnergy = 0;

    private final int mSampleRate;
    private final int mOneSec;

    // Recorder state
    private State mState;

    // Buffer size
    private int mBufferSize;

    // Number of frames written to byte array on each output
    private int mFramePeriod;

    // The complete space into which the recording in written.
    // Its maximum length is about:
    // 2 (bytes) * 1 (channels) * 30 (max rec time in seconds) * 44100 (times per second) = 2 646 000 bytes
    // but typically is:
    // 2 (bytes) * 1 (channels) * 20 (max rec time in seconds) * 16000 (times per second) = 640 000 bytes
    private final byte[] mRecording;

    private int mRecordedLength = 0;

    // Buffer for output
    private byte[] mBuffer;


    /**
     * <p>Instantiates a new recorder and sets the state to INITIALIZING.
     * In case of errors, no exception is thrown, but the state is set to ERROR.</p>
     *
     * <p>Android docs say: 44100Hz is currently the only rate that is guaranteed to work on all devices,
     * but other rates such as 22050, 16000, and 11025 may work on some devices.</p>
     *
     * @param audioSource Identifier of the audio source (e.g. microphone)
     * @param sampleRate Sample rate (e.g. 16000)
     */
    public RawAudioRecorder(int audioSource, int sampleRate) {
        mSampleRate = sampleRate;
        // E.g. 1 second of 16kHz 16-bit mono audio takes 32000 bytes.
        mOneSec = RESOLUTION_IN_BYTES * CHANNELS * mSampleRate;
        mRecording = new byte[mOneSec * MAX_RECORDING_TIME_IN_SECS];
        try {
            setBufferSizeAndFramePeriod();


            mRecorder = new AudioRecord(audioSource, mSampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION, mBufferSize);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                boolean agcAvailable = AutomaticGainControl.isAvailable();
                if(agcAvailable) {
                    AutomaticGainControl.create(mRecorder.getAudioSessionId());
                }
            }

            if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new Exception("AudioRecord initialization failed");
            }
            mBuffer = new byte[mFramePeriod * RESOLUTION_IN_BYTES * CHANNELS];
            setState(State.READY);
        } catch (Exception e) {
            release();
            setState(State.ERROR);
            if (e.getMessage() == null) {
                Log.e(LOG_TAG, "Unknown error occured while initializing recording");
            } else {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }


    public RawAudioRecorder(int sampleRate) {
        this(DEFAULT_AUDIO_SOURCE, sampleRate);
    }


    public RawAudioRecorder() {
        this(DEFAULT_AUDIO_SOURCE, DEFAULT_SAMPLE_RATE);
    }


    private int read(AudioRecord recorder) {
        int numberOfBytes = recorder.read(mBuffer, 0, mBuffer.length); // Fill buffer

        // Some error checking
        if (numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION) {
            Log.e(LOG_TAG, "The AudioRecord object was not properly initialized");
            return -1;
        } else if (numberOfBytes == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(LOG_TAG, "The parameters do not resolve to valid data and indexes.");
            return -2;
        } else if (numberOfBytes > mBuffer.length) {
            Log.e(LOG_TAG, "Read more bytes than is buffer length:" + numberOfBytes + ": " + mBuffer.length);
            return -3;
        } else if (numberOfBytes == 0) {
            Log.e(LOG_TAG, "Read zero bytes");
            return -4;
        }
        // Everything seems to be OK, adding the buffer to the recording.
        add(mBuffer);
        return 0;
    }

    private void setBufferSizeAndFramePeriod() {
        int minBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, RESOLUTION);
        if (minBufferSizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
            throw new IllegalArgumentException("AudioRecord.getMinBufferSize: parameters not supported by hardware");
        } else if (minBufferSizeInBytes == AudioRecord.ERROR) {
            Log.e(LOG_TAG, "AudioRecord.getMinBufferSize: unable to query hardware for output properties");
            minBufferSizeInBytes = mSampleRate * (120 / 1000) * RESOLUTION_IN_BYTES * CHANNELS;
        }
        mBufferSize = 2 * minBufferSizeInBytes;
        mFramePeriod = mBufferSize / ( 2 * RESOLUTION_IN_BYTES * CHANNELS );
        Log.i(LOG_TAG, "AudioRecord buffer size: " + mBufferSize + ", min size = " + minBufferSizeInBytes);
    }


    /**
     * @return recorder state
     */
    public State getState() {
        return mState;
    }

    private void setState(State state) {
        mState = state;
    }


    /**
     * @return bytes that have been recorded since the beginning
     */
    public byte[] getCompleteRecording() {
        return getCurrentRecording(0);
    }

    private byte[] getCurrentRecording(int startPos) {
        int len = getLength() - startPos;
        byte[] bytes = new byte[len];
        System.arraycopy(mRecording, startPos, bytes, 0, len);
        return bytes;
    }

    public int getLength() {
        return mRecordedLength;
    }


    /**
     * @return <code>true</code> iff a speech-ending pause has occurred at the end of the recorded data
     */
    public boolean isPausing() {
        double pauseScore = getPauseScore();
//		Log.i(LOG_TAG, "Pause score: " + pauseScore);
        return pauseScore > 7;
    }

    /**
     * @return volume indicator that shows the average volume of the last read buffer
     */
    public float getRmsdb() {
        long sumOfSquares = getRms(mRecordedLength, mBuffer.length);
        double rootMeanSquare = Math.sqrt(sumOfSquares / (mBuffer.length / 2));
        if (rootMeanSquare > 1) {
            Log.i(LOG_TAG, "getRmsdb(): " + rootMeanSquare);
            // TODO: why 10?
            return (float) (10 * Math.log10(rootMeanSquare));
        }
        return 0;
    }

    /**
     * <p>In order to calculate if the user has stopped speaking we take the
     * data from the last second of the recording, map it to a number
     * and compare this number to the numbers obtained previously. We
     * return a confidence score (0-INF) of a longer pause having occurred in the
     * speech input.</p>
     *
     * <p>TODO: base the implementation on some well-known technique.</p>
     *
     * @return positive value which the caller can use to determine if there is a pause
     */
    private double getPauseScore() {
        long t2 = getRms(mRecordedLength, mOneSec);
        if (t2 == 0) {
            return 0;
        }
        double t = mAvgEnergy / t2;
        mAvgEnergy = (2 * mAvgEnergy + t2) / 3;
        return t;
    }

    /**
     * <p>Stops the recording (if needed) and releases the resources.
     * The object can no longer be used and the reference should be
     * set to null after a call to release().</p>
     */
    public synchronized void release() {
        if (mRecorder != null) {
            if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                stop();
            }
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * <p>Starts the recording, and sets the state to RECORDING.</p>
     */
    public void start() {
        if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            mRecorder.startRecording();
            if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                setState(State.RECORDING);
                new Thread() {
                    public void run() {
                        while (mRecorder != null && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                            int status = read(mRecorder);
                            if (status < 0) {
                                break;
                            }
                        }
                    }
                }.start();
            } else {
                Log.e(LOG_TAG, "startRecording() failed");
                setState(State.ERROR);
            }
        } else {
            Log.e(LOG_TAG, "start() called on illegal state");
            setState(State.ERROR);
        }
    }


    /**
     * <p>Stops the recording, and sets the state to STOPPED.
     * If stopping fails then sets the state to ERROR.</p>
     */
    public void stop() {
        // We check the underlying AudioRecord state trying to avoid IllegalStateException.
        // If it still occurs then we catch it.
        if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED &&
                mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                mRecorder.stop();
                mRecorder.release();
                setState(State.STOPPED);
            } catch (IllegalStateException e) {
                Log.e(LOG_TAG, "native stop() called in illegal state: " + e.getMessage());
                setState(State.ERROR);
            }
        } else {
            Log.e(LOG_TAG, "stop() called in illegal state");
            setState(State.ERROR);
        }
    }


    /**
     * <p>Copy the given byte array into the total recording array.</p>
     *
     * <p>The total recording array has been pre-allocated (e.g. for 35 seconds of audio).
     * If it gets full then the recording is stopped.</p>
     *
     * @param buffer audio buffer
     */
    private void add(byte[] buffer) {
        if (mRecording.length >= mRecordedLength + buffer.length) {
            // arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            System.arraycopy(buffer, 0, mRecording, mRecordedLength, buffer.length);
            mRecordedLength += buffer.length;
        } else {
            // This also happens on the emulator for some reason
            Log.e(LOG_TAG, "Recorder buffer overflow: " + mRecordedLength);
            release();
        }
    }

    private long getRms(int end, int span) {
        int begin = end - span;
        if (begin < 0) {
            begin = 0;
        }
        // make sure begin is even
        if (0 != (begin % 2)) {
            begin++;
        }

        long sum = 0;
        for (int i = begin; i < end; i+=2) {
            byte argb1 = mRecording[i];
            byte argb2 = mRecording[i+1];
            short curSample = getShort(argb1, argb2);
            sum += curSample * curSample;
        }
        return sum;
    }


    /*
     * <p>Converts two bytes to a short, assuming that the 2nd byte is
     * more significant (LITTLE_ENDIAN format).</p>
     *
     * <pre>
     * 255 | (255 << 8)
     * 65535
     * </pre>
     */
    private static short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }
}
