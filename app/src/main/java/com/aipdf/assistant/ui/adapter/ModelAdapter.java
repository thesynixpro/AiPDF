package com.aipdf.assistant.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.models.AiModel;

import java.util.ArrayList;
import java.util.List;

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.VH> {

    public interface OnSelect {
        void onSelect(AiModel model);
    }

    private final List<AiModel> items = new ArrayList<>();
    private final OnSelect onSelect;

    public ModelAdapter(OnSelect onSelect) {
        this.onSelect = onSelect;
    }

    public void submit(List<AiModel> models) {
        items.clear();
        if (models != null) items.addAll(models);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AiModel m = items.get(position);
        holder.name.setText(m.displayName);
        holder.id.setText(m.id);

        if (m.description != null && !m.description.isEmpty()) {
            holder.desc.setVisibility(View.VISIBLE);
            holder.desc.setText(m.description.trim());
        } else {
            holder.desc.setVisibility(View.GONE);
        }

        List<String> caps = new ArrayList<>();
        if (m.contextLabel() != null) caps.add(m.contextLabel());
        if (m.providerLabel != null && !m.providerLabel.isEmpty()) caps.add(m.providerLabel);
        if (caps.isEmpty()) {
            // Capability metadata is not provided by the provider for this model.
            holder.caps.setVisibility(View.GONE);
        } else {
            holder.caps.setVisibility(View.VISIBLE);
            holder.caps.setText(String.join(" · ", caps));
        }

        holder.itemView.setOnClickListener(v -> {
            if (onSelect != null) onSelect.onSelect(m);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView id;
        final TextView desc;
        final TextView caps;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.model_name);
            id = itemView.findViewById(R.id.model_id);
            desc = itemView.findViewById(R.id.model_desc);
            caps = itemView.findViewById(R.id.model_caps);
        }
    }
}
