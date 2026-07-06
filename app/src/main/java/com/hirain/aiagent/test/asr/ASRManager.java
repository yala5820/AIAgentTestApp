package com.hirain.aiagent.test.asr;

import android.Manifest;
import android.app.Activity;
import android.util.Log;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;
import com.hirain.aiagent.test.R;
import com.iflytek.sparkchain.core.LogLvl;
import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;
import com.iflytek.sparkchain.core.asr.ASR;
import com.iflytek.sparkchain.core.asr.AsrCallbacks;
import com.iflytek.sparkchain.core.asr.Segment;
import com.iflytek.sparkchain.core.asr.Transcription;
import com.iflytek.sparkchain.core.asr.Vad;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ASRManager implements AudioRecorderManager.AudioDataCallback {
    private static final String TAG = "ASRManager";
    private final Activity activity;
    private final String language = "zh_cn";
    private final AtomicBoolean isWrite = new AtomicBoolean(false);
    int count = 0;
    private boolean isAuth = false;
    private boolean isrun = false;
    private boolean isdws = false;
    private volatile boolean isCanceled = false;
    private AudioRecorderManager audioRecorderManager;
    private String startMode = "NONE";
    private ASR mAsr = null;
    private OnAutomaticSpeechRecognitionListener onAutomaticSpeechRecognitionListener;

    public void setOnAutomaticSpeechRecognitionListener(OnAutomaticSpeechRecognitionListener listener) {
        this.onAutomaticSpeechRecognitionListener = listener;
    }

    public AsrCallbacks mAsrCallbacks = new AsrCallbacks() {
        @Override
        public void onResult(ASR.ASRResult asrResult, Object o) {
            if (isCanceled) return;

            int status = asrResult.getStatus();
            String result = asrResult.getBestMatchText();

            if (onAutomaticSpeechRecognitionListener != null) {
                if (status == 2) {
                    onAutomaticSpeechRecognitionListener.onResult(result);
                }
            }
        }

        @Override
        public void onError(ASR.ASRError asrError, Object o) {
            if (isCanceled) return;

            int code = asrError.getCode();
            String msg = asrError.getErrMsg();
            Log.e(TAG, "ASR error: code=" + code + " msg=" + msg);
            if (onAutomaticSpeechRecognitionListener != null) {
                onAutomaticSpeechRecognitionListener.onError();
            }
            stopAsr();
            isrun = false;
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d(TAG, "onBeginOfSpeech");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
        }
    };

    public ASRManager(Activity activity) {
        this.activity = activity;
    }

    public interface OnInitListener {
        void onInitSuccess();
        void onInitFailed();
    }

    /** 初始化 SDK + 申请 INTERNET 权限，通过回调通知结果 */
    public void getPermissionInit(OnInitListener listener) {
        XXPermissions.with(activity)
                .permission(Manifest.permission.INTERNET)
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean all) {
                        if (all) {
                            SDKInit();
                            if (isAuth) {
                                if (listener != null) listener.onInitSuccess();
                            } else {
                                if (listener != null) listener.onInitFailed();
                            }
                        }
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        Log.e(TAG, "INTERNET permission denied");
                        if (listener != null) listener.onInitFailed();
                    }
                });
    }

    private void SDKInit() {
        String logPath = activity.getApplicationContext().getFilesDir().getAbsolutePath() + "/SparkChain.log";
        SparkChainConfig config = SparkChainConfig.builder()
                .appID(activity.getString(R.string.iflytek_appid))
                .apiKey(activity.getString(R.string.iflytek_apikey))
                .apiSecret(activity.getString(R.string.iflytek_apisecret))
                .logPath(logPath)
                .logLevel(LogLvl.VERBOSE.getValue());

        int ret = SparkChain.getInst().init(activity.getApplicationContext(), config);
        isAuth = (ret == 0);
        if (!isAuth) {
            Log.e(TAG, "SparkChain SDK init failed, ret=" + ret);
        }
    }

    /** 启动录音 — 权限已在进入语音模式前完成，此处只检查 SDK 就绪状态 */
    public void startAsr() {
        if (!isAuth) {
            if (onAutomaticSpeechRecognitionListener != null)
                onAutomaticSpeechRecognitionListener.onError();
            return;
        }
        isCanceled = false;
        runAsr_Audio();
    }

    private void runAsr_Audio() {
        if (isrun) {
            Log.w(TAG, "ASR already running");
            return;
        }
        if (mAsr == null) {
            mAsr = new ASR();
            mAsr.registerCallbacks(mAsrCallbacks);
        }
        isdws = false;
        mAsr.language(language);
        mAsr.domain("iat");
        mAsr.accent("mandarin");
        mAsr.vinfo(true);
        if ("zh_cn".equals(language)) {
            mAsr.dwa("wpgs");
            isdws = true;
        }
        count++;
        int ret = mAsr.start(count + "");
        if (ret != 0) {
            isrun = false;
            if (onAutomaticSpeechRecognitionListener != null) {
                onAutomaticSpeechRecognitionListener.onError();
            }
            stopAsr();
        } else {
            isrun = true;
            isWrite.set(true);
            if (audioRecorderManager == null) {
                audioRecorderManager = AudioRecorderManager.getInstance();
            }
            audioRecorderManager.startRecord();
            audioRecorderManager.registerCallBack(this);
        }
    }

    /** 正常停止（false）/ 取消停止（true） */
    public void stopAsr(boolean cancel) {
        // 先切断录音数据流
        isWrite.set(false);
        if (audioRecorderManager != null) {
            audioRecorderManager.stopRecord();
            audioRecorderManager = null;
        }
        // 再停止 ASR 引擎
        if (mAsr != null) {
            if (cancel) {
                isCanceled = true;
                mAsr.stop(true);
            } else {
                mAsr.stop(false);
            }
        }
        isrun = false;
        startMode = "NONE";
    }

    public void stopAsr() {
        stopAsr(false);
    }

    @Override
    public void onAudioData(byte[] data, int size) {
        if (isWrite.get()) {
            int ret = mAsr.write(data);
            if (ret != 0) {
                isWrite.set(false);
            }
        }
    }

    @Override
    public void onAudioVolume(double db, int volume) {
    }

    public void destroy() {
        if (mAsr != null) {
            mAsr = null;
        }
    }

    public interface OnAutomaticSpeechRecognitionListener {
        void onResult(String str);
        void onError();
    }
}
