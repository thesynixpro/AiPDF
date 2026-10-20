package com.aipdf.assistant.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ui.home.ToolDef;

import java.util.ArrayList;
import java.util.List;

public class ToolRowAdapter extends RecyclerView.Adapter<ToolRowAdapter.VH> {

    public interface OnClick {
        void onClick(String toolId);
    }

    private final List<ToolDef> items = new ArrayList<>();
    private final OnClick onClick;

    public ToolRowAdapter(List<ToolDef> tools, OnClick onClick) {
        if (tools != null) items.addAll(tools);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ToolDef tool = items.get(position);
        holder.name.setText(tool.name);
        holder.desc.setText(tool.description);
        holder.icon.setImageResource(tool.iconRes);
        holder.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(tool.id);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView desc;
        final ImageView icon;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tool_name);
            desc = itemView.findViewById(R.id.tool_desc);
            icon = itemView.findViewById(R.id.tool_icon);
        }
    }
}
