package com.aipdf.assistant.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.AiCallback;
import com.aipdf.assistant.ai.AiError;
import com.aipdf.assistant.ai.AiProvider;
import com.aipdf.assistant.ai.ProviderManager;
import com.aipdf.assistant.ai.models.ChatMessage;
import com.aipdf.assistant.ai.models.DocumentSpec;
import com.aipdf.assistant.ai.models.DocumentSpecParser;
import com.aipdf.assistant.data.entity.MessageEntity;
import com.aipdf.assistant.data.repo.ChatRepository;
import com.aipdf.assistant.databinding.ActivityChatDetailBinding;
import com.aipdf.assistant.ui.preview.DocumentPreviewActivity;
import com.aipdf.assistant.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/** Reopens a stored conversation and continues chatting. */
public class ChatDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "conversation_id";

    private ActivityChatDetailBinding binding;
    private ChatAdapter adapter;
    private ChatRepository chatRepository;
    private ProviderManager providers;
    private long conversationId;

    public static Intent intentFor(Context context, long id) {
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra(EXTRA_ID, id);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        conversationId = getIntent().getLongExtra(EXTRA_ID, -1);
        chatRepository = new ChatRepository(this);
        providers = ProviderManager.getInstance(this);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new ChatAdapter(new ChatAdapter.Listener() {
            @Override
            public void onCopy(ChatAdapter.Item item) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("AI response", item.text));
            }

            @Override
            public void onGeneratePdf(ChatAdapter.Item item) {
                DocumentSpec spec = item.spec != null ? item.spec : DocumentSpecParser.parse(item.text);
                if (spec == null) return;
                startActivity(DocumentPreviewActivity.intentFor(ChatDetailActivity.this,
                        DocumentSpecParser.toJson(spec)));
            }

            @Override
            public void onRetry(ChatAdapter.Item item) {
            }
        });

        binding.messages.setLayoutManager(new LinearLayoutManager(this));
        binding.messages.setAdapter(adapter);
        binding.btnSend.setOnClickListener(v -> send());
        binding.btnAttach.setVisibility(View.GONE);

        if (conversationId > 0) {
            com.aipdf.assistant.data.entity.ConversationEntity c =
                    chatRepository.findConversation(conversationId);
            if (c != null) {
                binding.toolbar.setTitle(c.title);
            }
            loadMessages();
        }
    }

    private void loadMessages() {
        chatRepository.loadMessages(conversationId, messages -> {
            List<ChatAdapter.Item> items = new ArrayList<>();
            for (MessageEntity m : messages) {
                if (MessageEntity.class != null && "user".equals(m.role)) {
                    items.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_USER, m.content));
                } else if (m.documentJson) {
                    DocumentSpec spec = DocumentSpecParser.fromJsonString(m.content);
                    if (spec != null) {
                        items.add(ChatAdapter.Item.document(spec, m.content));
                    } else {
                        items.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT, m.content));
                    }
                } else {
                    items.add(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT, m.content));
                }
            }
            adapter.addAll(items);
            if (adapter.getItemCount() > 0) {
                binding.messages.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    private void send() {
        String text = binding.input.getText() == null ? "" : binding.input.getText().toString().trim();
        if (text.isEmpty()) return;
        binding.input.setText("");
        adaptAddAndPersist(new ChatAdapter.Item(ChatAdapter.Item.TYPE_USER, text), "user", false);

        AiProvider provider = providers.getActiveProvider();
        if (!providers.isReady(provider)) {
            adaptAddAndPersist(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ERROR,
                    getString(R.string.ai_not_configured)), "assistant", false);
            return;
        }
        binding.generatingBar.setVisibility(View.VISIBLE);
        List<ChatMessage> history = new ArrayList<>();
        for (ChatAdapter.Item item : adapter.snapshot()) {
            if (item.type == ChatAdapter.Item.TYPE_USER) history.add(ChatMessage.user(item.text));
            else if (item.type == ChatAdapter.Item.TYPE_ASSISTANT) history.add(ChatMessage.assistant(item.text));
        }
        provider.chat(history, null, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                binding.generatingBar.setVisibility(View.GONE);
                adaptAddAndPersist(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ASSISTANT, result),
                        "assistant", false);
            }

            @Override
            public void onError(AiError error) {
                binding.generatingBar.setVisibility(View.GONE);
                adaptAddAndPersist(new ChatAdapter.Item(ChatAdapter.Item.TYPE_ERROR,
                        error.userMessage(ChatDetailActivity.this)), "assistant", false);
            }
        });
    }

    private void adaptAddAndPersist(ChatAdapter.Item item, String role, boolean isDoc) {
        adapter.add(item);
        MessageEntity m = new MessageEntity();
        m.conversationId = conversationId;
        m.role = role;
        m.content = item.text;
        m.documentJson = isDoc;
        m.createdAt = System.currentTimeMillis();
        chatRepository.addMessage(m);
        binding.messages.post(() -> binding.messages.scrollToPosition(adapter.getItemCount() - 1));
    }
}
