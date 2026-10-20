package com.aipdf.assistant.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.aipdf.assistant.R;
import com.aipdf.assistant.data.AppDatabase;
import com.aipdf.assistant.databinding.FragmentSettingsBinding;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.settings.ProviderIds;
import com.aipdf.assistant.util.AppExecutors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAiSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AiSettingsActivity.class)));

        binding.switchDark.setChecked(AppSettings.isDarkMode());
        binding.switchDark.setOnCheckedChangeListener((b, checked) ->
                AppSettings.setDarkMode(checked ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO));

        bindPageSize();
        bindOrientation();
        binding.inputLanguage.setText(AppSettings.getDocumentLanguage());

        binding.btnClearHistory.setOnClickListener(v -> confirmClearHistory());
        binding.btnClearCache.setOnClickListener(v -> clearCache());

        binding.version.setText(getString(R.string.settings_version, "1.0"));
    }

    private void bindPageSize() {
        String[] values = new String[]{"A4", "Letter", "Legal"};
        binding.dropdownPageSize.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, values));
        binding.dropdownPageSize.setText(AppSettings.getPageSize().name(), false);
        binding.dropdownPageSize.setOnItemClickListener((parent, v, position, id) -> {
            AppSettings.PageSize size = AppSettings.PageSize.values()[position];
            AppSettings.setPageSize(size);
        });
    }

    private void bindOrientation() {
        String[] values = new String[]{"Portrait", "Landscape"};
        binding.dropdownOrientation.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, values));
        binding.dropdownOrientation.setText(
                AppSettings.getOrientation().name().toLowerCase(java.util.Locale.US), false);
        binding.dropdownOrientation.setOnItemClickListener((parent, v, position, id) -> {
            AppSettings.Orientation orientation = AppSettings.Orientation.values()[position];
            AppSettings.setOrientation(orientation);
        });
    }

    private void confirmClearHistory() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_clear_history)
                .setMessage(R.string.settings_clear_history_desc)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    AppExecutors.disk(() -> {
                        AppDatabase.get().clearAllTables();
                        AppExecutors.main(() -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.settings_history_cleared,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void clearCache() {
        AppExecutors.disk(() -> {
            deleteRecursive(new java.io.File(requireContext().getCacheDir(), "tmp"));
            AppExecutors.main(() -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.settings_cache_cleared,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deleteRecursive(java.io.File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) deleteRecursive(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) return;
        String provider = ProviderIds.displayName(AppSettings.getDefaultProviderId());
        String model = AppSettings.getModelFor(AppSettings.getDefaultProviderId());
        binding.aiStatus.setText(getString(R.string.settings_ai_summary, provider,
                model == null || model.isEmpty() ? "—" : model));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
