package com.hirain.aiagent.test.asr;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecorderManager {
    private final String TAG = "AudioRecorder";
    private int sampleRateInHz = 16000;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int channels = AudioFormat.CHANNEL_IN_MONO;
    private int bufferSize;
    private static AudioRecorderManager mInstance;
    private AudioRecord mRecorder;
    private AtomicBoolean isStart = new AtomicBoolean();
    private Thread recordThread;
    private AudioDataCallback callback;

    public AudioRecorderManager() {
        this(16000, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.CHANNEL_IN_MONO);
    }

    public AudioRecorderManager(int sampleRateInHz, int audioFormat, int channels) {
        this.sampleRateInHz = sampleRateInHz;
        this.audioFormat = audioFormat;
        this.channels = channels;
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channels, audioFormat);
        try {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channels, audioFormat, bufferSize);
        } catch (SecurityException e) {
            Log.e(TAG, "AudioRecord init failed: RECORD_AUDIO not granted", e);
            mRecorder = null;
        }
    }

    public void registerCallBack(AudioDataCallback callback) {
        this.callback = callback;
    }

    public static AudioRecorderManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioRecorderManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioRecorderManager();
                }
            }
        }
        return mInstance;
    }

    public void destroyThread() {
        synchronized (this) {
            try {
                isStart.set(false);
                if (null != recordThread && recordThread.isAlive()) {
                    try {
                        recordThread.interrupt();
                        recordThread.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        recordThread = null;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                recordThread = null;
            }
        }
    }

    private void startThread() {
        destroyThread();
        isStart.set(true);
        if (recordThread == null) {
            recordThread = new Thread(recordRunnable);
            recordThread.start();
        }
    }

    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mRecorder == null) return;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                int bytesRecord;
                byte[] tempBuffer = new byte[bufferSize];
                if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    stopRecord();
                    return;
                }
                mRecorder.startRecording();
                while (isStart.get()) {
                    synchronized (this) {
                        if (null != mRecorder) {
                            bytesRecord = mRecorder.read(tempBuffer, 0, bufferSize);
                            if (bytesRecord == AudioRecord.ERROR_INVALID_OPERATION || bytesRecord == AudioRecord.ERROR_BAD_VALUE) {
                                continue;
                            }
                            if (bytesRecord != 0 && bytesRecord != -1 && isStart.get()) {
                                double sumSquares = 0.0;
                                int sampleCount = bytesRecord / 2;
                                for (int i = 0; i < bytesRecord; i += 2) {
                                    short sample = (short) ((tempBuffer[i] & 0xFF) | ((tempBuffer[i + 1] & 0xFF) << 8));
                                    sumSquares += (double) sample * sample;
                                }
                                double rms = Math.sqrt(sumSquares / sampleCount);
                                double db = -120.0;
                                if (rms > 1e-10) {
                                    db = 20 * Math.log10(rms / 32767.0);
                                }
                                int volume = 0;
                                if (db > -60) {
                                    volume = (int) Math.min(9, Math.max(0, (db + 60) * 9 / 40.0));
                                }
                                if (callback != null) {
                                    callback.onAudioVolume(db, volume);
                                    callback.onAudioData(tempBuffer, bytesRecord);
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "录音异常:" + e.toString());
            } finally {
                if (mRecorder != null) {
                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                }
            }
        }
    };

    public void startRecord() {
        if (mRecorder == null) return;
        try {
            startThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        destroyThread();
        synchronized (this) {
            try {
                if (callback != null) {
                    callback = null;
                }
                if (mRecorder != null) {
                    if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        mRecorder.stop();
                    }
                    if (mRecorder != null) {
                        mRecorder.release();
                    }
                    mRecorder = null;
                }
                if (mInstance != null) {
                    mInstance = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mInstance = null;
            }
        }
    }

    public interface AudioDataCallback {
        void onAudioData(byte[] data, int size);
        void onAudioVolume(double db, int volume);
    }
}
