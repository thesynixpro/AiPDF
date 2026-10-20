package com.aipdf.assistant.ui.preview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.models.DocumentSpec;

import java.util.ArrayList;
import java.util.List;

/** Flattens a DocumentSpec into readable preview blocks. */
public class SpecPreviewAdapter extends RecyclerView.Adapter<SpecPreviewAdapter.VH> {

    private static class Block {
        String label;
        String text;
    }

    private final List<Block> blocks = new ArrayList<>();

    public void submit(DocumentSpec spec) {
        blocks.clear();
        if (spec == null) {
            notifyDataSetChanged();
            return;
        }
        Block cover = new Block();
        cover.label = "Title";
        cover.text = (spec.title == null ? "" : spec.title)
                + (spec.subtitle != null && !spec.subtitle.isEmpty() ? "\n" + spec.subtitle : "")
                + (spec.author != null && !spec.author.isEmpty() ? "\n— " + spec.author : "");
        blocks.add(cover);

        if (spec.tableOfContents) {
            Block toc = new Block();
            toc.label = "Table of contents";
            StringBuilder sb = new StringBuilder();
            int i = 1;
            if (spec.sections != null) {
                for (DocumentSpec.Section s : spec.sections) {
                    sb.append(i).append(". ").append(s.heading == null ? "" : s.heading).append("\n");
                    i++;
                }
            }
            toc.text = sb.toString().trim();
            blocks.add(toc);
        }

        if (spec.sections != null) {
            for (DocumentSpec.Section section : spec.sections) {
                Block h = new Block();
                h.label = "Section";
                h.text = section.heading == null ? "" : section.heading;
                blocks.add(h);
                if (section.elements == null) continue;
                for (DocumentSpec.Element el : section.elements) {
                    blocks.add(describe(el));
                }
            }
        }
        notifyDataSetChanged();
    }

    private Block describe(DocumentSpec.Element el) {
        Block b = new Block();
        switch (el.type) {
            case "heading":
                b.label = "Heading";
                b.text = el.text;
                break;
            case "list":
                b.label = el.ordered ? "Numbered list" : "Bullet list";
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (String item : el.items) {
                    sb.append(el.ordered ? (i + ". ") : "• ").append(item).append("\n");
                    i++;
                }
                b.text = sb.toString().trim();
                break;
            case "table":
                b.label = "Table (" + el.rows.size() + " rows)";
                StringBuilder tb = new StringBuilder();
                if (el.headers != null && !el.headers.isEmpty()) {
                    tb.append(String.join(" | ", el.headers)).append("\n");
                }
                for (List<String> row : el.rows) {
                    tb.append(String.join(" | ", row)).append("\n");
                }
                b.text = tb.toString().trim();
                break;
            case "pageBreak":
                b.label = "";
                b.text = "―――― page break ――――";
                break;
            case "paragraph":
            default:
                b.label = "Paragraph";
                b.text = el.text;
                break;
        }
        return b;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spec_block, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Block b = blocks.get(position);
        holder.label.setText(b.label);
        holder.label.setVisibility(b.label == null || b.label.isEmpty() ? View.GONE : View.VISIBLE);
        holder.text.setText(b.text);
    }

    @Override
    public int getItemCount() {
        return blocks.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView label;
        final TextView text;

        VH(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.block_label);
            text = itemView.findViewById(R.id.block_text);
        }
    }
}
