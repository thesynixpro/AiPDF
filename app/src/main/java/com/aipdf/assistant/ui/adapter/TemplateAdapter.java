package com.aipdf.assistant.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.pdf.DocumentTemplates;

import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.VH> {

    public interface Listener {
        void onUseTemplate(DocumentTemplates.Template template);

        void onFillWithAi(DocumentTemplates.Template template);
    }

    private final List<DocumentTemplates.Template> items = new ArrayList<>();
    private final Listener listener;

    public TemplateAdapter(List<DocumentTemplates.Template> templates, Listener listener) {
        if (templates != null) items.addAll(templates);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentTemplates.Template t = items.get(position);
        holder.name.setText(t.name);
        holder.desc.setText(t.description);
        holder.btnManual.setOnClickListener(v -> {
            if (listener != null) listener.onUseTemplate(t);
        });
        holder.btnAi.setOnClickListener(v -> {
            if (listener != null) listener.onFillWithAi(t);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView desc;
        final View btnAi;
        final View btnManual;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.template_name);
            desc = itemView.findViewById(R.id.template_desc);
            btnAi = itemView.findViewById(R.id.btn_ai_fill);
            btnManual = itemView.findViewById(R.id.btn_manual);
        }
    }
}
