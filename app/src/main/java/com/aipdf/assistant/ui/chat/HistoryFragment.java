package com.aipdf.assistant.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.databinding.FragmentHistoryBinding;
import com.aipdf.assistant.data.entity.ConversationEntity;
import com.aipdf.assistant.data.repo.ChatRepository;
import com.aipdf.assistant.ui.adapter.ConversationAdapter;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private ChatRepository chatRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        chatRepository = new ChatRepository(requireContext());
        ConversationAdapter adapter = new ConversationAdapter(new ConversationAdapter.Listener() {
            @Override
            public void onOpen(ConversationEntity conversation) {
                startActivity(ChatDetailActivity.intentFor(requireContext(), conversation.id));
            }

            @Override
            public void onDelete(ConversationEntity conversation) {
                chatRepository.deleteConversation(conversation);
            }
        });
        binding.chatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatList.setAdapter(adapter);
        chatRepository.observeConversations().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            binding.empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
