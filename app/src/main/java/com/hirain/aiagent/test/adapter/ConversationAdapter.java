package com.hirain.aiagent.test.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hirain.aiagent.ConversationInfo;
import com.hirain.aiagent.test.R;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<ConversationInfo> conversations = new ArrayList<>();
    private String activeSessionId;
    private OnItemClickListener onItemClick;
    private OnItemLongClickListener onItemLongClick;
    private boolean clickable = true;

    public interface OnItemClickListener {
        void onItemClick(ConversationInfo conversation);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(ConversationInfo conversation);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClick = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClick = listener;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public void submitList(List<ConversationInfo> list, String activeSessionId) {
        this.conversations.clear();
        if (list != null) {
            this.conversations.addAll(list);
        }
        this.activeSessionId = activeSessionId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationInfo conv = conversations.get(position);
        String title = conv.getTitle();
        if (title == null || title.isEmpty()) {
            String sid = conv.getSessionId();
            title = sid != null && sid.length() > 8 ? sid.substring(sid.length() - 8) : sid;
        }
        holder.tvTitle.setText(title);

        StringBuilder subtitle = new StringBuilder();
        if (conv.getPersonaId() != null) {
            subtitle.append(conv.getPersonaId());
        }
        if (conv.getMessageCount() > 0) {
            if (subtitle.length() > 0) subtitle.append(" · ");
            subtitle.append(conv.getMessageCount()).append(" 条");
        }
        holder.tvSubtitle.setText(subtitle.toString());

        boolean isActive = conv.getSessionId() != null && conv.getSessionId().equals(activeSessionId);
        if (isActive) {
            holder.itemView.setBackgroundResource(R.drawable.bg_conversation_selected);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!clickable) return;
            if (onItemClick != null) onItemClick.onItemClick(conv);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (!clickable) return false;
            if (onItemLongClick != null) return onItemLongClick.onItemLongClick(conv);
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_conversation_title);
            tvSubtitle = itemView.findViewById(R.id.tv_conversation_subtitle);
        }
    }
}
