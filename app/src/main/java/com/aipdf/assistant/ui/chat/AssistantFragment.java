package com.aipdf.assistant.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aipdf.assistant.R;
import com.aipdf.assistant.databinding.FragmentAssistantBinding;
import com.google.android.material.tabs.TabLayout;

public class AssistantFragment extends Fragment {

    private static final String TAG_CHAT = "assistant_chat";
    private static final String TAG_HISTORY = "assistant_history";

    private static String pendingPrompt;
    private static String pendingDocumentName;
    private static String pendingDocumentPath;

    private FragmentAssistantBinding binding;
    private ChatFragment chatFragment;

    /** Requests that the chat tab open with this prompt prefilled. */
    public static void startFromPrompt(String prompt) {
        pendingPrompt = prompt;
        pendingDocumentName = null;
        pendingDocumentPath = null;
    }

    /** Requests that the chat tab open attached to a document. */
    public static void startWithDocument(String name, String path) {
        pendingDocumentName = name;
        pendingDocumentPath = path;
        pendingPrompt = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.ai_tab_chat));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.ai_tab_history));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        showTab(0);
    }

    private void showTab(int position) {
        if (position == 0) {
            chatFragment = new ChatFragment();
            if (pendingPrompt != null) {
                chatFragment.setPendingPrompt(pendingPrompt);
                pendingPrompt = null;
            }
            if (pendingDocumentPath != null) {
                chatFragment.setPendingDocument(pendingDocumentName, pendingDocumentPath);
                pendingDocumentPath = null;
                pendingDocumentName = null;
            }
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.content, chatFragment, TAG_CHAT)
                    .commitNow();
        } else {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.content, new HistoryFragment(), TAG_HISTORY)
                    .commitNow();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
