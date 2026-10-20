package com.aipdf.assistant.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ui.home.ToolDef;
import com.aipdf.assistant.ui.home.ToolNavigator;

import java.util.ArrayList;
import java.util.List;

/** Horizontal cards used on the Home screen for popular tools. */
public class ToolCardAdapter extends RecyclerView.Adapter<ToolCardAdapter.VH> {

    public interface OnClick {
        void onClick(String toolId);
    }

    private final List<ToolDef> items = new ArrayList<>();
    private final OnClick onClick;

    public ToolCardAdapter(List<ToolDef> tools, OnClick onClick) {
        if (tools != null) items.addAll(tools);
        this.onClick = onClick;
    }

    public static ToolCardAdapter forHome(Context context, OnClick onClick) {
        List<ToolDef> tools = new ArrayList<>();
        tools.add(new ToolDef(ToolNavigator.TOOL_TEXT_TO_PDF, context.getString(R.string.tool_text_to_pdf),
                context.getString(R.string.tool_desc_text), R.drawable.ic_text));
        tools.add(new ToolDef(ToolNavigator.TOOL_MERGE, context.getString(R.string.tool_merge),
                context.getString(R.string.tool_desc_merge), R.drawable.ic_merge));
        tools.add(new ToolDef(ToolNavigator.TOOL_SPLIT, context.getString(R.string.tool_split),
                context.getString(R.string.tool_desc_split), R.drawable.ic_split));
        tools.add(new ToolDef(ToolNavigator.TOOL_COMPRESS, context.getString(R.string.tool_compress),
                context.getString(R.string.tool_desc_compress), R.drawable.ic_compress));
        tools.add(new ToolDef(ToolNavigator.TOOL_PROTECT, context.getString(R.string.tool_protect),
                context.getString(R.string.tool_desc_protect), R.drawable.ic_lock));
        return new ToolCardAdapter(tools, onClick);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ToolDef tool = items.get(position);
        holder.name.setText(tool.name);
        holder.desc.setText(tool.description);
        holder.iconView.setImageResource(tool.iconRes);
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
        final ImageView iconView;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tool_name);
            desc = itemView.findViewById(R.id.tool_desc);
            iconView = itemView.findViewById(R.id.tool_icon);
        }
    }
}
