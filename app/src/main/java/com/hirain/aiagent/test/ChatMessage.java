package com.hirain.aiagent.test;

public class ChatMessage {
    public static final int TYPE_SENT = 0;
    public static final int TYPE_RECEIVED = 1;

    private final int type;
    private final String content;
    private final long timestamp;

    public ChatMessage(int type, String content, long timestamp) {
        this.type = type;
        this.content = content;
        this.timestamp = timestamp;
    }

    public int getType() { return type; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
