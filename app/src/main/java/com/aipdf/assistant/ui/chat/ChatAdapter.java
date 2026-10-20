package com.aipdf.assistant.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.models.DocumentSpec;
import com.aipdf.assistant.ai.models.DocumentSpecParser;

import java.util.ArrayList;
import java.util.List;

/** Renders chat bubbles for user, assistant and error messages. */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    public static class Item {
        public static final int TYPE_USER = 0;
        public static final int TYPE_ASSISTANT = 1;
        public static final int TYPE_ERROR = 2;
        public static final int TYPE_DOCUMENT = 3;

        public final int type;
        public String text;
        public DocumentSpec spec;
        public boolean streaming;
        public long messageId;

        public Item(int type, String text) {
            this.type = type;
            this.text = text;
        }

        public static Item document(DocumentSpec spec, String raw) {
            Item item = new Item(TYPE_DOCUMENT, raw);
            item.spec = spec;
            return item;
        }
    }

    public interface Listener {
        void onCopy(Item item);

        void onGeneratePdf(Item item);

        void onRetry(Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private final Listener listener;

    public ChatAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(false);
    }

    public void add(Item item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void updateLast(Item item) {
        if (items.isEmpty()) {
            add(item);
            return;
        }
        int index = items.size() - 1;
        items.set(index, item);
        notifyItemChanged(index);
    }

    public Item last() {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Item> restored) {
        items.clear();
        items.addAll(restored);
        notifyDataSetChanged();
    }

    public List<Item> snapshot() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Item item = items.get(position);
        boolean isUser = item.type == Item.TYPE_USER;
        boolean isError = item.type == Item.TYPE_ERROR;

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        params.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
        holder.bubble.setLayoutParams(params);

        holder.bubble.setBackgroundResource(isUser ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_ai);
        holder.text.setTextColor(isUser
                ? holder.itemView.getContext().getColor(R.color.md_on_primary)
                : (isError
                ? holder.itemView.getContext().getColor(R.color.md_error)
                : holder.itemView.getContext().getColor(com.google.android.material.R.color.m3_sys_color_light_on_surface)));

        if (item.type == Item.TYPE_DOCUMENT && item.spec != null) {
            holder.text.setText(buildDocumentSummary(item.spec));
        } else {
            holder.text.setText(item.text);
        }
        holder.typing.setVisibility(item.streaming && !isUser ? View.VISIBLE : View.GONE);

        boolean showActions = !isUser && !item.streaming && item.text != null && !item.text.isEmpty();
        holder.actions.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.btnCopy.setOnClickListener(v -> {
            if (listener != null) listener.onCopy(item);
        });
        holder.btnPdf.setOnClickListener(v -> {
            if (listener != null) listener.onGeneratePdf(item);
        });
        holder.btnRetry.setVisibility(isError ? View.VISIBLE : View.GONE);
        holder.btnRetry.setOnClickListener(v -> {
            if (listener != null) listener.onRetry(item);
        });
    }

    private String buildDocumentSummary(DocumentSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u2714 ").append(spec.title == null ? "Document" : spec.title).append("\n");
        int sections = spec.sections == null ? 0 : spec.sections.size();
        int elements = spec.estimateElementCount();
        sb.append(sections).append(" sections · ").append(elements).append(" content blocks");
        sb.append("\n\n").append("Tap \"Generate PDF\" to export this as a formatted PDF file.");
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final LinearLayout bubble;
        final TextView text;
        final ProgressBar typing;
        final LinearLayout actions;
        final View btnCopy;
        final View btnPdf;
        final View btnRetry;

        VH(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.bubble);
            text = itemView.findViewById(R.id.text);
            typing = itemView.findViewById(R.id.typing);
            actions = itemView.findViewById(R.id.actions);
            btnCopy = itemView.findViewById(R.id.btn_copy);
            btnPdf = itemView.findViewById(R.id.btn_pdf);
            btnRetry = itemView.findViewById(R.id.btn_retry);
        }
    }
}
