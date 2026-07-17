package com.hirain.aiagent.test.eval.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.TextView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hirain.aiagent.test.R;
import com.hirain.aiagent.test.eval.protocol.EvalAction;
import com.hirain.aiagent.test.eval.protocol.EvalCommand;
import com.hirain.aiagent.test.eval.protocol.EvalProtocolCodec;
import com.hirain.aiagent.test.eval.protocol.EvalResultEnvelope;
import com.hirain.aiagent.test.eval.runtime.EvalBridgeManager;
import java.nio.charset.StandardCharsets;

/** Debug-only 半自动入口。它只展示命令与结果，不显示环境 token。 */
public final class EvalBridgeActivity extends Activity {
    private EditText commandInput; private TextView status; private TextView result;
    private EvalBridgeManager manager;
    @Override protected void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_eval_bridge); commandInput = findViewById(R.id.eval_command); status = findViewById(R.id.eval_status); result = findViewById(R.id.eval_result); manager = EvalBridgeManager.get(this); manager.setObserver(this::showResult); findViewById(R.id.eval_connect).setOnClickListener(v -> { manager.connect(); status.setText("正在连接 Debug Eval Service"); }); findViewById(R.id.eval_execute).setOnClickListener(v -> submit(commandInput.getText().toString())); findViewById(R.id.eval_acquire).setOnClickListener(v -> submitAction(EvalAction.ACQUIRE_ENVIRONMENT)); findViewById(R.id.eval_release).setOnClickListener(v -> submitAction(EvalAction.RELEASE_ENVIRONMENT)); handleIntent(); }
    @Override protected void onNewIntent(android.content.Intent intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(); }
    @Override protected void onDestroy() { if (manager != null) manager.setObserver(null); super.onDestroy(); }
    private void handleIntent() { String encoded = getIntent().getStringExtra("eval_command_b64"); if (encoded == null) return; try { String json = new String(Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8); if (json.getBytes(StandardCharsets.UTF_8).length > 16 * 1024) throw new IllegalArgumentException(); commandInput.setText(json); if (getIntent().getBooleanExtra("eval_auto_execute", false)) submit(json); } catch (Exception e) { status.setText("命令解码失败或超过 16KiB"); } }
    private void submitAction(EvalAction action) { try { EvalProtocolCodec.DecodedCommand decoded = new EvalProtocolCodec().decodeCommand(commandInput.getText().toString()); decoded.command.action = action; decoded.raw.addProperty("action", action.name()); manager.submit(decoded.raw.toString()); } catch (Exception e) { status.setText("请先输入合法命令 JSON"); } }
    private void submit(String json) { manager.submit(json); status.setText("命令已提交"); }
    private void showResult(EvalResultEnvelope envelope, String path) { runOnUiThread(() -> { status.setText("状态: " + envelope.bridgeState); result.setText("correlationId=" + envelope.correlationId + "\n结果文件=" + path + "\n错误=" + (envelope.bridgeError == null ? "无" : envelope.bridgeError.code)); }); }
}
