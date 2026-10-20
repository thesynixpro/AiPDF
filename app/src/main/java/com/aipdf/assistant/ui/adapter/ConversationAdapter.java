package com.aipdf.assistant.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.ConversationEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {

    public interface Listener {
        void onOpen(ConversationEntity conversation);

        void onDelete(ConversationEntity conversation);
    }

    private final List<ConversationEntity> items = new ArrayList<>();
    private final Listener listener;

    public ConversationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ConversationEntity> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ConversationEntity c = items.get(position);
        holder.title.setText(c.title);
        String date = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date(c.updatedAt));
        String meta = date;
        if (c.attachedDocumentName != null) meta += " · " + c.attachedDocumentName;
        holder.meta.setText(meta);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(c);
        });
        holder.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        final ImageButton delete;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.conv_title);
            meta = itemView.findViewById(R.id.conv_meta);
            delete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
