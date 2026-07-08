package com.hirain.aiagent.test.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hirain.aiagent.AIAgent;
import com.hirain.aiagent.AgentRequest;
import com.hirain.aiagent.AgentResponse;
import com.hirain.aiagent.CancelRequestResult;
import com.hirain.aiagent.ConversationInfo;
import com.hirain.aiagent.ConversationListResponse;
import com.hirain.aiagent.ConversationOperationResult;
import com.hirain.aiagent.ConversationRequest;
import com.hirain.aiagent.IAIAgentServiceListener;
import com.hirain.aiagent.test.R;
import com.hirain.aiagent.test.adapter.ChatAdapter;
import com.hirain.aiagent.test.asr.ASRManager;
import com.hirain.aiagent.test.model.ChatMessage;
import com.hirain.aiagent.test.adapter.ConversationAdapter;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements IAIAgentServiceListener {

    private static final String TAG = "AIAgentTest";
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
    private TextView tvConnectionStatus;
    private DrawerLayout drawerLayout;
    private ChatAdapter chatAdapter;

    // Sidebar
    private Button mBtnNewConversation;
    private TextView mTvDrawerEmpty;
    private RecyclerView mRvConversations;
    private Button mBtnDrawerUser;
    private Button mBtnDrawerAiSettings;
    private ConversationAdapter conversationAdapter;

    // 状态
    private String currentUserId = "default_user";
    private String currentPersonaId = "chat";
    private String currentSessionId;
    private ConversationInfo currentConversation;
    private String activeRequestId;
    private String activeClientMessageId;
    private boolean isRequestProcessing = false;
    private Handler requestWatchdog = new Handler(Looper.getMainLooper());
    private Runnable watchdogRunnable = () -> {
        Log.w(TAG, "Request watchdog timeout");
        setRequestProcessing(false, null);
        Toast.makeText(MainActivity.this, "请求等待超时，请检查 AIAgent 服务状态", Toast.LENGTH_LONG).show();
    };

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
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        mBtnNewConversation = findViewById(R.id.btn_new_conversation);
        mTvDrawerEmpty = findViewById(R.id.tv_drawer_empty);
        mRvConversations = findViewById(R.id.rv_conversations);
        mBtnDrawerUser = findViewById(R.id.btn_drawer_user);
        mBtnDrawerAiSettings = findViewById(R.id.btn_drawer_ai_settings);
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText("连接中…");
        }

        // 侧边栏会话列表
        conversationAdapter = new ConversationAdapter();
        mRvConversations.setLayoutManager(new LinearLayoutManager(this));
        mRvConversations.setAdapter(conversationAdapter);
        conversationAdapter.setOnItemClickListener(conv -> {
            if (isRequestProcessing) {
                Toast.makeText(this, "当前请求处理中，请先等待完成", Toast.LENGTH_SHORT).show();
                return;
            }
            ConversationOperationResult result = AIAgent.getInstance().switchConversation(currentUserId, conv.getSessionId());
            if (result != null && result.isSuccess()) {
                currentSessionId = conv.getSessionId();
                currentConversation = conv;
                ttsPrefs.edit().putString("current_session_id", currentSessionId).apply();
                chatAdapter.clearMessages();
                chatAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_RECEIVED,
                        getString(R.string.conversation_switch_hint) + conv.getTitle(), System.currentTimeMillis()));
                drawerLayout.closeDrawer(GravityCompat.START);
                refreshHeaderState();
            } else {
                Toast.makeText(this, "切换会话失败", Toast.LENGTH_SHORT).show();
            }
        });
        conversationAdapter.setOnItemLongClickListener(conv -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.conversation_delete_title)
                    .setMessage(R.string.conversation_delete_confirm)
                    .setPositiveButton("确定", (dialog, which) -> {
                        ConversationOperationResult r = AIAgent.getInstance().deleteConversation(currentUserId, conv.getSessionId());
                        if (r != null && r.isSuccess()) {
                            if (conv.getSessionId().equals(currentSessionId)) {
                                currentSessionId = null;
                                currentConversation = null;
                                ttsPrefs.edit().remove("current_session_id").apply();
                                chatAdapter.clearMessages();
                                chatAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_RECEIVED,
                                        getString(R.string.conversation_deleted), System.currentTimeMillis()));
                                refreshHeaderState();
                            }
                            loadConversationsForCurrentUser();
                        } else {
                            Toast.makeText(this, "删除会话失败", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
        refreshDrawerState(0);

        // 侧边栏按钮点击
        mBtnNewConversation.setOnClickListener(v -> {
            if (isRequestProcessing) {
                Toast.makeText(this, "当前请求处理中，请先停止或等待完成", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = "新对话 " + new java.text.SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    .format(new java.util.Date());
            ConversationRequest convReq = new ConversationRequest();
            convReq.setUserId(currentUserId);
            convReq.setPersonaId(currentPersonaId);
            convReq.setTitle(title);
            convReq.setSourceApp("aiagent_test");
            convReq.setTimestamp(System.currentTimeMillis());
            ConversationOperationResult result = AIAgent.getInstance().createConversation(convReq);
            if (result != null && result.isSuccess()) {
                currentSessionId = result.getConversationInfo().getSessionId();
                currentConversation = null;
                ttsPrefs.edit().putString("current_session_id", currentSessionId).apply();
                chatAdapter.clearMessages();
                chatAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_RECEIVED,
                        getString(R.string.conversation_new_created) + title, System.currentTimeMillis()));
                loadConversationsForCurrentUser();
                drawerLayout.closeDrawer(GravityCompat.START);
                refreshHeaderState();
            } else {
                Toast.makeText(this, "新建会话失败", Toast.LENGTH_SHORT).show();
            }
        });
        mBtnDrawerUser.setOnClickListener(v -> {
            if (isRequestProcessing) {
                Toast.makeText(this, "当前请求处理中，请先停止或等待完成", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] users = {"default_user", "test_user_1", "test_user_2"};
            int checked = 0;
            for (int i = 0; i < users.length; i++) {
                if (users[i].equals(currentUserId)) { checked = i; break; }
            }
            new AlertDialog.Builder(this)
                    .setTitle("切换用户")
                    .setSingleChoiceItems(users, checked, (dialog, which) -> {
                        String newUserId = users[which];
                        currentUserId = newUserId;
                        ttsPrefs.edit().putString("current_user_id", newUserId).apply();
                        currentSessionId = null;
                        currentConversation = null;
                        ttsPrefs.edit().remove("current_session_id").apply();
                        chatAdapter.clearMessages();
                        chatAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_RECEIVED,
                                getString(R.string.user_switched) + newUserId, System.currentTimeMillis()));
                        loadConversationsForCurrentUser();
                        dialog.dismiss();
                    })
                    .show();
        });
        mBtnDrawerAiSettings.setOnClickListener(v -> {
            if (isRequestProcessing) {
                Toast.makeText(this, "当前请求处理中，请先停止或等待完成", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] personas = {"chat（默认）", "friendly（友好）", "concise（简洁）"};
            String[] personaValues = {"chat", "friendly", "concise"};
            int checkedPersona = 0;
            for (int i = 0; i < personaValues.length; i++) {
                if (personaValues[i].equals(currentPersonaId)) { checkedPersona = i; break; }
            }
            new AlertDialog.Builder(this)
                    .setTitle("AI 设置")
                    .setSingleChoiceItems(personas, checkedPersona, (dialog, which) -> {
                        String newPersona = personaValues[which];
                        currentPersonaId = newPersona;
                        ttsPrefs.edit().putString("current_persona_id", newPersona).apply();
                        chatAdapter.addMessage(new ChatMessage(ChatMessage.TYPE_RECEIVED,
                                getString(R.string.persona_switched) + personas[which], System.currentTimeMillis()));
                        dialog.dismiss();
                    })
                    .setNeutralButton("TTS 设置", (dialog, which) -> showTtsSettingsDialog())
                    .show();
        });

        // 恢复持久化状态
        isVoiceMode = ttsPrefs.getBoolean("voice_mode", false);
        currentUserId = ttsPrefs.getString("current_user_id", "default_user");
        currentPersonaId = ttsPrefs.getString("current_persona_id", "chat");
        currentSessionId = ttsPrefs.getString("current_session_id", null);
        // 持久化语音模式在重建后需重新初始化 SDK，默认回退到文本模式
        if (isVoiceMode) {
            isVoiceMode = false;
            ttsPrefs.edit().putBoolean("voice_mode", false).apply();
        }
        applyInputMode();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.CHINA);
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);
                if (isTtsReady) {
                    Log.d(TAG, "TTS engine initialized successfully");
                } else {
                    Log.w(TAG, "TTS init: Chinese language pack not installed (result=" + result + ")");
                    Toast.makeText(MainActivity.this,
                            "未安装中文语音包，语音回复将静默显示", Toast.LENGTH_LONG).show();
                }
            } else {
                isTtsReady = false;
                Log.e(TAG, "TTS engine binding failed, status=" + status + ". Retrying in 2s.");
                final TextToSpeech[] retryRef = new TextToSpeech[1];
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    retryRef[0] = new TextToSpeech(getApplicationContext(), retryStatus -> {
                        if (retryStatus == TextToSpeech.SUCCESS && retryRef[0] != null) {
                            TextToSpeech oldTts = textToSpeech;
                            int r = retryRef[0].setLanguage(Locale.CHINA);
                            isTtsReady = (r != TextToSpeech.LANG_MISSING_DATA
                                    && r != TextToSpeech.LANG_NOT_SUPPORTED);
                            if (isTtsReady) {
                                if (oldTts != null && oldTts != retryRef[0]) {
                                    oldTts.shutdown();
                                }
                                textToSpeech = retryRef[0];
                            }
                        }
                    });
                }, 2000L);
                Toast.makeText(MainActivity.this,
                        "语音引擎连接中…", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initAsr() {
        asrManager = new ASRManager(this);
        asrManager.setOnAutomaticSpeechRecognitionListener(new ASRManager.OnAutomaticSpeechRecognitionListener() {
            @Override
            public void onResult(String text) {
                runOnUiThread(() -> {
                    // 恢复 DrawerLayout 手势
                    if (drawerLayout != null) {
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }
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
                    // 恢复 DrawerLayout 手势
                    if (drawerLayout != null) {
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }
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
            // 语音模式：右侧按钮保留可见，禁用（发送由语音条松手触发）
            btnSend.setVisibility(View.VISIBLE);
            btnSend.setEnabled(false);
            btnSend.setAlpha(0.4f);
            btnSend.setImageResource(R.drawable.ic_send);
            tvVoiceBar.setText(R.string.voice_bar_idle);
            tvVoiceBar.setBackgroundResource(R.drawable.bg_voice_bar_normal);
        } else {
            btnToggleInput.setImageResource(R.drawable.ic_mic);
            tvVoiceBar.setVisibility(View.GONE);
            etInput.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);
            btnSend.setEnabled(true);
            btnSend.setAlpha(1.0f);
            btnSend.setImageResource(R.drawable.ic_send);
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
                    // 录音期间锁定 DrawerLayout，防止侧滑干扰
                    if (drawerLayout != null) {
                        drawerLayout.requestDisallowInterceptTouchEvent(true);
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    }
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
                        // 恢复 DrawerLayout 手势
                        if (drawerLayout != null) {
                            drawerLayout.requestDisallowInterceptTouchEvent(false);
                            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
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
            if (isRequestProcessing) {
                // 停止当前请求
                CancelRequestResult cancelResult = AIAgent.getInstance()
                        .cancelAgentRequest(activeRequestId, "cancelled_by_client");
                if (cancelResult != null && CancelRequestResult.STATUS_ACCEPTED.equals(cancelResult.getStatus())) {
                    btnSend.setEnabled(false);
                    btnSend.setAlpha(0.4f);
                    Toast.makeText(MainActivity.this, "取消中…", Toast.LENGTH_SHORT).show();
                } else {
                    boolean isDone = cancelResult != null
                            && (CancelRequestResult.STATUS_NOT_FOUND.equals(cancelResult.getStatus())
                             || CancelRequestResult.STATUS_ALREADY_FINISHED.equals(cancelResult.getStatus()));
                    if (isDone) {
                        setRequestProcessing(false, null);
                        Toast.makeText(MainActivity.this, "请求已完成", Toast.LENGTH_SHORT).show();
                    } else {
                        setRequestProcessing(false, null);
                        Toast.makeText(MainActivity.this, "取消请求失败", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                String text = etInput.getText().toString().trim();
                if (!TextUtils.isEmpty(text)) {
                    etInput.setText("");
                    sendTextRequest(text, false);
                }
            }
        });

        btnToggleInput.setOnClickListener(v -> toggleInputMode());

        btnSettings.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        tvVoiceBar.setOnTouchListener(voiceBarTouchListener);
    }

    private void sendTextRequest(String text, boolean fromVoice) {
        if (currentSessionId == null) {
            Toast.makeText(this, "请先新建或选择会话", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRequestProcessing) {
            Toast.makeText(this, "当前请求处理中，请先等待完成", Toast.LENGTH_SHORT).show();
            return;
        }
        chatAdapter.addMessage(new ChatMessage(
                ChatMessage.TYPE_SENT, text, System.currentTimeMillis()));
        scrollToBottom();

        String requestId = UUID.randomUUID().toString();
        String clientMessageId = UUID.randomUUID().toString();

        AgentRequest req = new AgentRequest();
        req.setRequestId(requestId);
        req.setClientMessageId(clientMessageId);
        req.setSessionId(currentSessionId);
        req.setUserId(currentUserId);
        req.setPersonaId(currentPersonaId);
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
            shouldSpeak = fromVoice;
        }
        requestTtsMap.put(requestId, shouldSpeak);

        int ret = AIAgent.getInstance().processAgentRequest(req);
        if (ret != 0) {
            requestTtsMap.remove(requestId);
            Toast.makeText(this, "AIAgent 服务未连接", Toast.LENGTH_SHORT).show();
        } else {
            setRequestProcessing(true, requestId, clientMessageId);
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
            if (tvConnectionStatus != null) tvConnectionStatus.setText("已连接");
            initActiveConversation();
            loadConversationsForCurrentUser();
        });
    }

    @Override
    public void onAIAgentServiceDisconnected() {
        Log.d(TAG, "AIAgentService 断开");
        runOnUiThread(() -> {
            if (tvConnectionStatus != null) tvConnectionStatus.setText("未连接");
            requestWatchdog.removeCallbacks(watchdogRunnable);
            if (isRequestProcessing) {
                setRequestProcessing(false, null);
            }
        });
    }

    @Override
    public void onAIResponse(AgentResponse response) {
        if (response == null) return;
        String respReqId = response.getRequestId();
        boolean isCurrent = respReqId != null && respReqId.equals(activeRequestId);
        Boolean wasVoiceEntry = requestTtsMap.remove(respReqId);
        String text = response.getText();
        if (text == null) text = response.getErrorDetail() != null ? response.getErrorDetail() : "系统: 请求已结束";
        boolean isSuccess = response.isSuccess();
        String status = response.getStatus();
        final String displayText = text;
        final boolean shouldSpeak = isCurrent && wasVoiceEntry != null && wasVoiceEntry
                && isSuccess && !"CANCELLED".equals(status) && !"TIMEOUT".equals(status)
                && !"EXCEPTION".equals(status);
        runOnUiThread(() -> {
            // 终态恢复 — 现在在 UI 线程执行，不会因跨线程 UI 操作而出错
            if (isCurrent) {
                setRequestProcessing(false, null);
                chatAdapter.addMessage(new ChatMessage(
                        ChatMessage.TYPE_RECEIVED, displayText, System.currentTimeMillis()));
                scrollToBottom();
                if (shouldSpeak && isTtsReady && textToSpeech != null) {
                    textToSpeech.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, "ai_response");
                }
            } else {
                chatAdapter.addMessage(new ChatMessage(
                        ChatMessage.TYPE_RECEIVED, "（迟到响应）" + displayText, System.currentTimeMillis()));
                scrollToBottom();
            }
        });
    }

    private void setRequestProcessing(boolean processing, String requestId) {
        setRequestProcessing(processing, requestId, null);
    }

    private void setRequestProcessing(boolean processing, String requestId, String clientMessageId) {
        isRequestProcessing = processing;
        this.activeRequestId = requestId;
        this.activeClientMessageId = clientMessageId;

        // Watchdog：processing 时启动 20s 超时定时器
        requestWatchdog.removeCallbacks(watchdogRunnable);
        if (processing) {
            requestWatchdog.postDelayed(watchdogRunnable, 20000L);
        }

        // 处理中禁用侧边栏操作，恢复时重新启用
        mBtnNewConversation.setEnabled(!processing);
        mBtnDrawerUser.setEnabled(!processing);
        mBtnDrawerAiSettings.setEnabled(!processing);
        conversationAdapter.setClickable(!processing);

        if (processing) {
            btnSend.setImageResource(R.drawable.ic_stop);
            btnSend.setEnabled(true);
            btnSend.setAlpha(1.0f);
        } else {
            btnSend.setImageResource(R.drawable.ic_send);
            boolean isVoiceAndIdle = isVoiceMode;
            btnSend.setEnabled(!isVoiceAndIdle);
            btnSend.setAlpha(isVoiceAndIdle ? 0.4f : 1.0f);
        }
    }

    private void refreshDrawerState(int conversationCount) {
        if (conversationCount > 0) {
            mRvConversations.setVisibility(View.VISIBLE);
            mTvDrawerEmpty.setVisibility(View.GONE);
        } else {
            mRvConversations.setVisibility(View.GONE);
            mTvDrawerEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void loadConversationsForCurrentUser() {
        ConversationListResponse resp = AIAgent.getInstance().listConversations(currentUserId);
        if (resp != null && resp.isSuccess() && resp.getConversations() != null
                && currentUserId.equals(resp.getUserId())) {
            List<ConversationInfo> list = resp.getConversations();
            // 检查当前 sessionId 是否仍在列表中，不在则清空
            boolean sessionExists = currentSessionId == null;
            if (!sessionExists) {
                for (ConversationInfo c : list) {
                    if (currentSessionId.equals(c.getSessionId())) { sessionExists = true; break; }
                }
            }
            if (!sessionExists && currentSessionId != null) {
                currentSessionId = null;
                currentConversation = null;
                ttsPrefs.edit().remove("current_session_id").apply();
            }
            conversationAdapter.submitList(list, currentSessionId);
            refreshDrawerState(list.size());
        } else {
            refreshDrawerState(0);
        }
        refreshHeaderState();
    }

    private void initActiveConversation() {
        // 仅当本地没有 sessionId 时，从服务端获取 active 会话
        if (currentSessionId != null) return;
        ConversationInfo active = AIAgent.getInstance().getActiveConversation(currentUserId);
        if (active != null) {
            currentSessionId = active.getSessionId();
            currentConversation = active;
            ttsPrefs.edit().putString("current_session_id", currentSessionId).apply();
        }
    }

    private void refreshHeaderState() {
        String status = "已连接";
        if (currentUserId != null && currentSessionId != null) {
            status = currentUserId + " · " + currentSessionId.substring(Math.max(0, currentSessionId.length() - 8));
        } else if (currentUserId != null) {
            status = currentUserId;
        }
        if (tvConnectionStatus != null) tvConnectionStatus.setText(status);
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

        // Watchdog 清理
        requestWatchdog.removeCallbacks(watchdogRunnable);

        // 残留映射清理
        requestTtsMap.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从后台返回时，如果 AIAgent 服务持续运行（没有重新连接），刷新会话状态
        if (AIAgent.getInstance().isConnected()) {
            initActiveConversation();
            loadConversationsForCurrentUser();
        }
    }
}
