package com.aipdf.assistant.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.aipdf.assistant.R;
import com.aipdf.assistant.databinding.FragmentAssistantBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class AssistantFragment extends Fragment {

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
        binding.pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
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
                    return chatFragment;
                }
                return new HistoryFragment();
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) ->
                tab.setText(position == 0 ? R.string.ai_tab_chat : R.string.ai_tab_history))
                .attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
