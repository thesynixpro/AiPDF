package com.aipdf.assistant.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.util.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.VH> {

    public interface Listener {
        void onOpen(DocumentEntity doc);

        void onMore(DocumentEntity doc, View anchor);
    }

    private final List<DocumentEntity> items = new ArrayList<>();
    private final Listener listener;

    public DocumentAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<DocumentEntity> docs) {
        items.clear();
        if (docs != null) items.addAll(docs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentEntity doc = items.get(position);
        holder.name.setText(doc.name);
        String meta = holder.itemView.getContext().getString(R.string.files_pages, doc.pageCount)
                + " · " + FileUtils.humanSize(doc.sizeBytes);
        if (doc.generated) meta += " · AI";
        holder.meta.setText(meta);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(doc);
        });
        holder.more.setOnClickListener(v -> {
            if (listener != null) listener.onMore(doc, v);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;
        final ImageButton more;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doc_name);
            meta = itemView.findViewById(R.id.doc_meta);
            more = itemView.findViewById(R.id.btn_more);
        }
    }
}
