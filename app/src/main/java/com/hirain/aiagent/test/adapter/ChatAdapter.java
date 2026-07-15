package com.hirain.aiagent.test.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hirain.aiagent.test.R;
import com.hirain.aiagent.test.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();
    private int messageMaxWidthPx;

    /**
     * 更新消息最大宽度。宽度由 RecyclerView 的真实可用空间计算，放在 Adapter
     * 之外可以避免每个 ViewHolder 重复注册布局监听，也能在横竖屏变化时统一刷新。
     */
    public void setMessageMaxWidthPx(int messageMaxWidthPx) {
        if (messageMaxWidthPx <= 0 || this.messageMaxWidthPx == messageMaxWidthPx) {
            return;
        }
        this.messageMaxWidthPx = messageMaxWidthPx;
        if (!messages.isEmpty()) {
            notifyItemRangeChanged(0, messages.size());
        }
    }

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvMessage.setText(msg.getContent());
        if (messageMaxWidthPx > 0) {
            holder.tvMessage.setMaxWidth(messageMaxWidthPx);
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvMessage.getLayoutParams();
        if (msg.getType() == ChatMessage.TYPE_SENT) {
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_send);
            params.gravity = Gravity.END;
        } else {
            holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_receive);
            params.gravity = Gravity.START;
        }
        holder.tvMessage.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }
}
