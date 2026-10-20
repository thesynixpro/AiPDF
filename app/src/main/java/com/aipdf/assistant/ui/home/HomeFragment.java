package com.aipdf.assistant.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.databinding.FragmentHomeBinding;
import com.aipdf.assistant.ui.MainActivity;
import com.aipdf.assistant.ui.TextToPdfActivity;
import com.aipdf.assistant.ui.adapter.DocumentAdapter;
import com.aipdf.assistant.ui.adapter.ToolCardAdapter;
import com.aipdf.assistant.ui.reader.PdfReaderActivity;
import com.aipdf.assistant.ui.templates.TemplateGalleryActivity;
import com.aipdf.assistant.ui.chat.AssistantFragment;
import com.aipdf.assistant.util.PendingIntentBus;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DocumentRepository repository;
    private DocumentAdapter recentAdapter;

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    repository.importDocument(uri, new DocumentRepository.Callback<DocumentEntity>() {
                        @Override
                        public void onResult(DocumentEntity value) {
                            openDocument(value);
                        }

                        @Override
                        public void onError(String message) {
                            UiToast.error(requireContext(), message);
                        }
                    });
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new DocumentRepository(requireContext());

        recentAdapter = new DocumentAdapter(new DocumentAdapter.Listener() {
            @Override
            public void onOpen(DocumentEntity doc) {
                openDocument(doc);
            }

            @Override
            public void onMore(DocumentEntity doc, View anchor) {
                DocumentActions.showMenu(requireContext(), doc, repository, anchor);
            }
        });
        binding.recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentList.setAdapter(recentAdapter);

        binding.toolsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.toolsList.setAdapter(ToolCardAdapter.forHome(requireContext(), tool ->
                ToolNavigator.open(requireContext(), tool)));

        repository.observeRecent(5).observe(getViewLifecycleOwner(), docs -> {
            recentAdapter.submit(docs);
            binding.recentEmpty.setVisibility(docs == null || docs.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.btnCreateAi.setOnClickListener(v -> {
            String prompt = binding.promptInput.getText() == null ? "" : binding.promptInput.getText().toString().trim();
            AssistantFragment.startFromPrompt(prompt);
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openAssistant();
            }
        });

        binding.btnImport.setOnClickListener(v -> importLauncher.launch(new String[]{"application/pdf"}));

        binding.btnNewBlank.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), TextToPdfActivity.class));
        });

        binding.btnTemplates.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TemplateGalleryActivity.class)));

        binding.btnAllFiles.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openFiles();
            }
        });

        android.net.Uri pending = PendingIntentBus.consumePendingImportUri();
        if (pending != null) {
            repository.importDocument(pending, new DocumentRepository.Callback<DocumentEntity>() {
                @Override
                public void onResult(DocumentEntity value) {
                    openDocument(value);
                }

                @Override
                public void onError(String message) {
                    UiToast.error(requireContext(), message);
                }
            });
        }
    }

    private void openDocument(DocumentEntity doc) {
        Intent intent = PdfReaderActivity.intentFor(requireContext(), doc.id, doc.path, doc.name);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
