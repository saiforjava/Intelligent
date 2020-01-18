package com.sai.intelligent.utils;

import android.content.Context;
import android.media.AudioManager;
import android.os.Looper;
import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioRecorder {

    private final static int WAVE_HEADER_LENGTH = 44;

    private static AudioRecorder instance;

    private Context mContext;

    private volatile Looper mSendLooper;

    private RawAudioRecorder mRecorder;

    private OnErrorListener mOnErrorListener;

    private int mErrorCode;

    private long mStartTime = 0;
    private State mState = null;

    private byte[] waveHeaderTemplate;
    private short nChannels = 1;
    private short bSamples = 16;
    private int sRate = 8000;
    private int payloadSize;


    public static AudioRecorder getInstance(Context context) {
        //if(instance == null) {
        instance = new AudioRecorder(context);
        //}

        return instance;
    }

    private AudioRecorder(Context context) {
        this.mContext = context;

        setState(State.IDLE);
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    public State getState() {
        return mState;
    }

    private void setState(State state) {
        mState = state;
    }

    /**
     * @return time when the recording started
     */
    public long getStartTime() {
        return mStartTime;
    }

    /**
     * @return <code>true</code> iff currently recording or processing
     */
    public boolean isWorking() {
        State currentState = getState();
        return currentState == State.RECORDING || currentState == State.PROCESSING;
    }

    /**
     * @return length of the current recording in bytes
     */
    public int getLength() {
        if (mRecorder == null) {
            return 0;
        }
        return mRecorder.getLength();
    }

    /**
     * @return dB value of recent sound pressure
     */
    public float getRmsdb() {
        if (mRecorder == null) {
            return 0;
        }
        return mRecorder.getRmsdb();
    }

    /**
     * @return <code>true</code> iff currently recording non-speech
     */
    public boolean isPausing() {
        return mRecorder != null && mRecorder.isPausing();
    }

    /**
     * @return complete audio data from the beginning of the recording
     */
    public byte[] getCompleteRecording() {
        if (mRecorder == null) {
            return new byte[0];
        }

        byte[] completeRecord = mRecorder.getCompleteRecording();
        payloadSize = completeRecord.length;
        mountWaveHeaderTemplate();
        return appendLatestRecording(completeRecord);
    }

    public byte[] getRecording() {
        if (mRecorder == null) {
            return new byte[0];
        }

        return mRecorder.getCompleteRecording();
    }

    /**
     * @return error code that corresponds to the latest error state
     */
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * <p>Tries to create a speech recognition session.</p>
     *
     * @return <code>true</code> iff there was no error
     */
    public boolean init() {
        if (mState != State.IDLE && mState != State.ERROR) {
            processError(Constants.RESULT_CLIENT_ERROR, null);
            return false;
        }

        setState(State.INITIALIZED);
        return true;
    }

    /**
     * <p>Start recording with the given sample rate.</p>
     */
    public boolean start() {

        if (mState != State.INITIALIZED) {
            processError(Constants.RESULT_CLIENT_ERROR, null);
            return false;
        }

        try {
            startRecording();
            mStartTime = SystemClock.elapsedRealtime();
            setState(State.RECORDING);
            return true;
        } catch (IOException e) {
            processError(Constants.RESULT_AUDIO_ERROR, e);
        }
        return false;
    }

    /**
     * <p>Stops the recording, finishes chunk sending, sends off the
     * last chunk (in another thread).</p>
     */
    public boolean stop() {

        if (mState != State.RECORDING || mRecorder == null) {
            processError(Constants.RESULT_CLIENT_ERROR, null);
            return false;
        }

        mRecorder.stop();

        setState(State.IDLE);

        return true;
    }

    /**
     * <p>Starts recording from the microphone with the given sample rate.</p>
     *
     * @throws IOException if recorder could not be created
     */
    private void startRecording() throws IOException {
        AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mRecorder = new RawAudioRecorder(Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)));
        if (mRecorder.getState() == RawAudioRecorder.State.ERROR) {
            throw new IOException("Recording error");
        }

        if (mRecorder.getState() != RawAudioRecorder.State.READY) {
            throw new IOException("Recording ready");
        }

        mRecorder.start();

        if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
            throw new IOException("Recording in progress");
        }
    }

    /**
     * <p>We kill the running processes in this order:
     * chunk sending, recognizer session, audio recorder.</p>
     * <p/>
     * <p>Note that mRecorder.release() can be called in any state.
     * After that the recorder object is no longer available,
     * so we should set it to <code>null</code>.</p>
     */
    private void releaseResources() {
        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
//        if (mAudioPauser != null) {
//            mAudioPauser.resume();
//        }
    }

    private void processError(int errorCode, Exception e) {
        mErrorCode = errorCode;
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(errorCode, e);
        }
        releaseResources();
        setState(State.ERROR);
    }

    public enum State {
        // Service created or resources released
        IDLE,
        // Recognizer session created
        INITIALIZED,
        // Started the recording
        RECORDING,
        // Finished recording, transcribing now
        PROCESSING,
        // Got an error
        ERROR
    }

    public interface OnErrorListener {
        boolean onError(int errorCode, Exception e);
    }

    private byte[] appendLatestRecording(byte[] audioData) {

        final int phraseLength = WAVE_HEADER_LENGTH + payloadSize; // WAVE header + audio data length

        ByteArrayOutputStream bdbAudioData = new ByteArrayOutputStream(phraseLength);

        bdbAudioData.write(getWaveHeader(), 0, WAVE_HEADER_LENGTH); // Insert wave header of latest recording
        bdbAudioData.write(audioData, 0, audioData.length); // Insert the latest audio data

        return bdbAudioData.toByteArray();
    }

    // Add wave header to the beginning of the read samples
    private void mountWaveHeaderTemplate() {

        waveHeaderTemplate = new byte[WAVE_HEADER_LENGTH];

        waveHeaderTemplate[0] = 'R';
        waveHeaderTemplate[1] = 'I';
        waveHeaderTemplate[2] = 'F';
        waveHeaderTemplate[3] = 'F';

        int totalDataLen = 36 + payloadSize;
        waveHeaderTemplate[4] = (byte) (totalDataLen & 0xff);
        waveHeaderTemplate[5] = (byte) ((totalDataLen >> 8) & 0xff);
        waveHeaderTemplate[6] = (byte) ((totalDataLen >> 16) & 0xff);
        waveHeaderTemplate[7] = (byte) ((totalDataLen >> 24) & 0xff);

        waveHeaderTemplate[8] = 'W';
        waveHeaderTemplate[9] = 'A';
        waveHeaderTemplate[10] = 'V';
        waveHeaderTemplate[11] = 'E';
        waveHeaderTemplate[12] = 'f';  // 'fmt ' chunk
        waveHeaderTemplate[13] = 'm';
        waveHeaderTemplate[14] = 't';
        waveHeaderTemplate[15] = ' ';
        waveHeaderTemplate[16] = 16;  // 4 bytes: size of 'fmt ' chunk

        waveHeaderTemplate[17] = 0;
        waveHeaderTemplate[18] = 0;
        waveHeaderTemplate[19] = 0;
        waveHeaderTemplate[20] = 1;  // format = 1

        waveHeaderTemplate[21] = 0;
        waveHeaderTemplate[22] = (byte) nChannels;
        waveHeaderTemplate[23] = 0;
        waveHeaderTemplate[24] = (byte) (sRate & 0xff);
        waveHeaderTemplate[25] = (byte) ((sRate >> 8) & 0xff);
        waveHeaderTemplate[26] = (byte) ((sRate >> 16) & 0xff);
        waveHeaderTemplate[27] = (byte) ((sRate >> 24) & 0xff);

        int byteRate = sRate*bSamples*nChannels/8;
        waveHeaderTemplate[28] = (byte) (byteRate & 0xff);
        waveHeaderTemplate[29] = (byte) ((byteRate >> 8) & 0xff);
        waveHeaderTemplate[30] = (byte) ((byteRate >> 16) & 0xff);
        waveHeaderTemplate[31] = (byte) ((byteRate >> 24) & 0xff);

        waveHeaderTemplate[32] = (byte) (nChannels * bSamples / 8);  // block align

        waveHeaderTemplate[33] = 0;
        waveHeaderTemplate[34] = (byte) bSamples;  // bits per sample

        waveHeaderTemplate[35] = 0;
        waveHeaderTemplate[36] = 'd';
        waveHeaderTemplate[37] = 'a';
        waveHeaderTemplate[38] = 't';
        waveHeaderTemplate[39] = 'a';

        waveHeaderTemplate[40] = (byte) (payloadSize & 0xff);
        waveHeaderTemplate[41] = (byte) ((payloadSize >> 8) & 0xff);
        waveHeaderTemplate[42] = (byte) ((payloadSize >> 16) & 0xff);
        waveHeaderTemplate[43] = (byte) ((payloadSize >> 24) & 0xff);
    }

    private byte[] getWaveHeader() {
        return waveHeaderTemplate;
    }
}
