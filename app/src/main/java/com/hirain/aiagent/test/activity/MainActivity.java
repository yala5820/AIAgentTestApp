package com.hirain.aiagent.test.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hirain.aiagent.AIAgent;
import com.hirain.aiagent.AgentRequest;
import com.hirain.aiagent.AgentResponse;
import com.hirain.aiagent.IAIAgentServiceListener;
import com.hirain.aiagent.test.R;
import com.hirain.aiagent.test.adapter.ChatAdapter;
import com.hirain.aiagent.test.asr.ASRManager;
import com.hirain.aiagent.test.model.ChatMessage;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements IAIAgentServiceListener {

    private static final String TAG = "AIAgentTest";
    private static final String SESSION_ID = "test_user";
    private static final String SOURCE_APP = "aiagent_test";
    private static final float CANCEL_THRESHOLD_DP = 80f;

    // UI
    private RecyclerView rvChat;
    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnToggleInput;
    private ImageButton btnSettings;
    private TextView tvVoiceBar;
    private Toolbar toolbar;
    private ChatAdapter chatAdapter;

    // Voice
    private ASRManager asrManager;
    private boolean isVoiceMode = false;
    private boolean isRecording = false;
    private float touchStartY;

    // TTS
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private final Map<String, Boolean> requestTtsMap = new HashMap<>();
    private SharedPreferences ttsPrefs;
    private static final String TTS_MODE_KEY = "tts_mode";
    private static final String TTS_MODE_AUTO = "auto";
    private static final String TTS_MODE_ON = "on";
    private static final String TTS_MODE_OFF = "off";

    // ASR

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ttsPrefs = getSharedPreferences("tts_settings", MODE_PRIVATE);

        initViews();
        setupRecyclerView();
        setupListeners();
        initTts();
        initAsr();

        AIAgent.getInstance().registerAIAgentLisener(this);

        chatAdapter.addMessage(new ChatMessage(
                ChatMessage.TYPE_RECEIVED, "你好！我是 AI 测试助手，请开始对话。", System.currentTimeMillis()));
    }

    // ── 初始化 ──

    private void initViews() {
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnToggleInput = findViewById(R.id.btn_toggle_input);
        btnSettings = findViewById(R.id.btn_settings);
        tvVoiceBar = findViewById(R.id.tv_voice_bar);
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setSubtitle("连接中…");
        }

        // 恢复上次输入模式
        isVoiceMode = ttsPrefs.getBoolean("voice_mode", false);
        applyInputMode();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINA);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);
                if (!isTtsReady) {
                    Log.w(TAG, "TTS language not supported");
                }
            } else {
                Log.e(TAG, "TTS init failed, status=" + status);
            }
        });
    }

    private void initAsr() {
        asrManager = new ASRManager(this);
        asrManager.setOnAutomaticSpeechRecognitionListener(new ASRManager.OnAutomaticSpeechRecognitionListener() {
            @Override
            public void onResult(String text) {
                runOnUiThread(() -> {
                    Log.d(TAG, "ASR result: " + text);
                    isRecording = false;
                    tvVoiceBar.setText(R.string.voice_bar_idle);
                    tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_normal);
                    tvVoiceBar.setEnabled(true);

                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(MainActivity.this, "未识别到语音", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendTextRequest(text, true);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    isRecording = false;
                    tvVoiceBar.setText(R.string.voice_bar_idle);
                    tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_normal);
                    tvVoiceBar.setEnabled(true);
                    Toast.makeText(MainActivity.this, "语音识别失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ── 输入模式切换 ──

    private void toggleInputMode() {
        if (isVoiceMode) {
            // 切换到文字模式
            isVoiceMode = false;
            ttsPrefs.edit().putBoolean("voice_mode", false).apply();
            applyInputMode();
        } else {
            // 切换到语音模式 — 先初始化 SDK
            asrManager.getPermissionInit(new ASRManager.OnInitListener() {
                @Override
                public void onInitSuccess() {
                    // SDK 就绪后，检查 RECORD_AUDIO 权限
                    if (XXPermissions.hasPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        isVoiceMode = true;
                        ttsPrefs.edit().putBoolean("voice_mode", true).apply();
                        runOnUiThread(() -> applyInputMode());
                    } else {
                        XXPermissions.with(MainActivity.this)
                                .permission(Manifest.permission.RECORD_AUDIO)
                                .request(new OnPermission() {
                                    @Override
                                    public void hasPermission(List<String> granted, boolean all) {
                                        if (all) {
                                            isVoiceMode = true;
                                            ttsPrefs.edit().putBoolean("voice_mode", true).apply();
                                            runOnUiThread(() -> applyInputMode());
                                        }
                                    }

                                    @Override
                                    public void noPermission(List<String> denied, boolean quick) {
                                        runOnUiThread(() ->
                                                Toast.makeText(MainActivity.this, "需要录音权限才能使用语音输入", Toast.LENGTH_SHORT).show());
                                    }
                                });
                    }
                }

                @Override
                public void onInitFailed() {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "语音引擎初始化失败", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void applyInputMode() {
        if (isVoiceMode) {
            btnToggleInput.setImageResource(R.drawable.ic_keyboard);
            etInput.setVisibility(View.GONE);
            tvVoiceBar.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.GONE);
            tvVoiceBar.setText(R.string.voice_bar_idle);
            tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_normal);
        } else {
            btnToggleInput.setImageResource(R.drawable.ic_mic);
            tvVoiceBar.setVisibility(View.GONE);
            etInput.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);
        }
    }

    // ── 语音条触摸事件（微信式长按） ──

    private final View.OnTouchListener voiceBarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 长按开始录音
                    touchStartY = event.getRawY();
                    isRecording = true;
                    tvVoiceBar.setText(R.string.voice_bar_recording);
                    tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_recording);
                    asrManager.startAsr();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isRecording) {
                        float diffY = touchStartY - event.getRawY();
                        float threshold = CANCEL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                        if (diffY > threshold) {
                            tvVoiceBar.setText(R.string.voice_bar_cancel);
                            tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_cancel);
                        } else {
                            tvVoiceBar.setText(R.string.voice_bar_recording);
                            tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_recording);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isRecording) {
                        float diffY = touchStartY - event.getRawY();
                        float threshold = CANCEL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                        if (diffY > threshold) {
                            // 上滑取消
                            asrManager.stopAsr(true);
                            isRecording = false;
                            tvVoiceBar.setText(R.string.voice_bar_idle);
                            tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_normal);
                        } else {
                            // 正常发送 — 等待 ASR 回调
                            tvVoiceBar.setText("识别中…");
                            tvVoiceBar.setEnabled(false);
                            asrManager.stopAsr(false);
                        }
                    }
                    return true;
            }
            return false;
        }
    };

    // ── 发送 ──

    private void setupListeners() {
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            etInput.setText("");
            sendTextRequest(text, false);
        });

        btnToggleInput.setOnClickListener(v -> toggleInputMode());

        btnSettings.setOnClickListener(v -> {
            // TTS 设置弹窗
            showTtsSettingsDialog();
        });

        tvVoiceBar.setOnTouchListener(voiceBarTouchListener);
    }

    private void sendTextRequest(String text, boolean fromVoice) {
        chatAdapter.addMessage(new ChatMessage(
                ChatMessage.TYPE_SENT, text, System.currentTimeMillis()));
        scrollToBottom();

        AgentRequest req = new AgentRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setSessionId(SESSION_ID);
        req.setSourceApp(SOURCE_APP);
        req.setText(text);
        req.setInputType("TEXT");
        req.setTimestamp(System.currentTimeMillis());

        // 计算是否播报 TTS
        String mode = ttsPrefs.getString(TTS_MODE_KEY, TTS_MODE_AUTO);
        boolean shouldSpeak;
        if (TTS_MODE_OFF.equals(mode)) {
            shouldSpeak = false;
        } else if (TTS_MODE_ON.equals(mode)) {
            shouldSpeak = true;
        } else {
            shouldSpeak = fromVoice; // auto 模式：仅语音输入播报
        }
        requestTtsMap.put(req.getRequestId(), shouldSpeak);

        int ret = AIAgent.getInstance().processAgentRequest(req);
        if (ret != 0) {
            requestTtsMap.remove(req.getRequestId());
            Toast.makeText(this, "AIAgent 服务未连接", Toast.LENGTH_SHORT).show();
        }
    }

    // ── TTS 设置弹窗 ──

    private void showTtsSettingsDialog() {
        String[] modes = {"自动（仅语音输入）", "强制开启", "强制关闭"};
        String[] values = {TTS_MODE_AUTO, TTS_MODE_ON, TTS_MODE_OFF};
        String current = ttsPrefs.getString(TTS_MODE_KEY, TTS_MODE_AUTO);
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("TTS 播报设置")
                .setSingleChoiceItems(modes, checked, (dialog, which) -> {
                    ttsPrefs.edit().putString(TTS_MODE_KEY, values[which]).apply();
                    dialog.dismiss();
                })
                .show();
    }

    // ── IAIAgentServiceListener ──

    @Override
    public void onAIAgentServiceConnected() {
        Log.d(TAG, "AIAgentService 连接成功");
        runOnUiThread(() -> {
            if (toolbar != null) toolbar.setSubtitle("已连接");
        });
    }

    @Override
    public void onAIAgentServiceDisconnected() {
        Log.d(TAG, "AIAgentService 断开");
        runOnUiThread(() -> {
            if (toolbar != null) toolbar.setSubtitle("未连接");
        });
    }

    @Override
    public void onAIResponse(AgentResponse response) {
        if (response == null || response.getText() == null) return;
        runOnUiThread(() -> {
            String text = response.getText();
            chatAdapter.addMessage(new ChatMessage(
                    ChatMessage.TYPE_RECEIVED, text, System.currentTimeMillis()));
            scrollToBottom();

            // TTS 播报
            Boolean shouldSpeak = requestTtsMap.remove(response.getRequestId());
            if (shouldSpeak != null && shouldSpeak && isTtsReady && textToSpeech != null) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ai_response");
            }
        });
    }

    private void scrollToBottom() {
        rvChat.post(() -> rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    // ── 生命周期 ──

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AIAgent.getInstance().unRegisterAIAgentLisener(this);

        // TTS 清理
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isTtsReady = false;

        // ASR 清理
        if (asrManager != null) {
            asrManager.stopAsr();
            asrManager.destroy();
        }

        // 残留映射清理
        requestTtsMap.clear();
    }
}
