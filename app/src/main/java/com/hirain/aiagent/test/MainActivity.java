package com.hirain.aiagent.test;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hirain.aiagent.AIAgent;
import com.hirain.aiagent.AgentRequest;
import com.hirain.aiagent.AgentResponse;
import com.hirain.aiagent.IAIAgentServiceListener;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements IAIAgentServiceListener {

    private static final String TAG = "AIAgentTest";
    private static final String SESSION_ID = "test_user";
    private static final String SOURCE_APP = "aiagent_test";

    private RecyclerView rvChat;
    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnSettings;
    private Toolbar toolbar;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupListeners();

        AIAgent.getInstance().registerAIAgentLisener(this);

        chatAdapter.addMessage(new ChatMessage(
                ChatMessage.TYPE_RECEIVED, "你好！我是 AI 测试助手，请开始对话。", System.currentTimeMillis()));
    }

    private void initViews() {
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        btnSettings = findViewById(R.id.btn_settings);
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setSubtitle("连接中…");
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendTextMessage());

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void sendTextMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etInput.setText("");

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

        int ret = AIAgent.getInstance().processAgentRequest(req);
        if (ret != 0) {
            Log.w(TAG, "processAgentRequest returned " + ret + ", service may not be connected");
            Toast.makeText(this, "AIAgent 服务未连接", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        rvChat.post(() -> rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    // ======== IAIAgentServiceListener ========

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
            chatAdapter.addMessage(new ChatMessage(
                    ChatMessage.TYPE_RECEIVED, response.getText(), System.currentTimeMillis()));
            scrollToBottom();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AIAgent.getInstance().unRegisterAIAgentLisener(this);
    }
}
