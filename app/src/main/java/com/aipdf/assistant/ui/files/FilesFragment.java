package com.aipdf.assistant.ui.files;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.entity.DocumentEntity;
import com.aipdf.assistant.data.repo.DocumentRepository;
import com.aipdf.assistant.databinding.FragmentFilesBinding;
import com.aipdf.assistant.ui.adapter.DocumentAdapter;
import com.aipdf.assistant.ui.home.DocumentActions;
import com.aipdf.assistant.ui.home.UiToast;
import com.aipdf.assistant.ui.reader.PdfReaderActivity;
import com.aipdf.assistant.util.PendingIntentBus;

import java.util.List;

public class FilesFragment extends Fragment {

    private FragmentFilesBinding binding;
    private DocumentRepository repository;
    private DocumentAdapter adapter;
    private LiveData<List<DocumentEntity>> currentSource;
    private final Observer<List<DocumentEntity>> observer = docs -> {
        adapter.submit(docs);
        binding.empty.setVisibility(docs == null || docs.isEmpty() ? View.VISIBLE : View.GONE);
    };
    private String sortMode = "date";
    private String query = "";

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {
                    }
                    importUri(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFilesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new DocumentRepository(requireContext());
        adapter = new DocumentAdapter(new DocumentAdapter.Listener() {
            @Override
            public void onOpen(DocumentEntity doc) {
                openDocument(doc);
            }

            @Override
            public void onMore(DocumentEntity doc, View anchor) {
                DocumentActions.showMenu(requireContext(), doc, repository, anchor);
            }
        });
        binding.docList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.docList.setAdapter(adapter);

        binding.fabImport.setOnClickListener(v -> importLauncher.launch(new String[]{"application/pdf"}));
        binding.btnSort.setOnClickListener(this::showSortMenu);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString().trim();
                refreshSource();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        refreshSource();

        Uri pending = PendingIntentBus.consumePendingImportUri();
        if (pending != null) importUri(pending);
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, getString(R.string.files_sort_name));
        menu.getMenu().add(0, 2, 1, getString(R.string.files_sort_date));
        menu.getMenu().add(0, 3, 2, getString(R.string.files_sort_size));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) sortMode = "name";
            else if (item.getItemId() == 2) sortMode = "date";
            else sortMode = "size";
            refreshSource();
            return true;
        });
        menu.show();
    }

    private void refreshSource() {
        if (currentSource != null) currentSource.removeObserver(observer);
        if (!query.isEmpty()) {
            currentSource = repository.dao().searchByName(query);
        } else if ("name".equals(sortMode)) {
            currentSource = repository.dao().observeByName();
        } else if ("size".equals(sortMode)) {
            currentSource = repository.dao().observeBySize();
        } else {
            currentSource = repository.dao().observeByDate();
        }
        currentSource.observe(getViewLifecycleOwner(), observer);
    }

    private void importUri(Uri uri) {
        repository.importDocument(uri, new DocumentRepository.Callback<DocumentEntity>() {
            @Override
            public void onResult(DocumentEntity value) {
                UiToast.info(requireContext(), getString(R.string.files_import_success));
            }

            @Override
            public void onError(String message) {
                UiToast.error(requireContext(), message);
            }
        });
    }

    private void openDocument(DocumentEntity doc) {
        startActivity(PdfReaderActivity.intentFor(requireContext(), doc.id, doc.path, doc.name));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentSource != null) currentSource.removeObserver(observer);
        binding = null;
    }
}
