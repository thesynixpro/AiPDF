package com.aipdf.assistant.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.ai.AiCallback;
import com.aipdf.assistant.ai.AiError;
import com.aipdf.assistant.ai.AiProvider;
import com.aipdf.assistant.ai.ProviderManager;
import com.aipdf.assistant.ai.models.AiModel;
import com.aipdf.assistant.ai.models.ChatMessage;
import com.aipdf.assistant.databinding.ActivityAiSettingsBinding;
import com.aipdf.assistant.security.SecureKeyStore;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.settings.ProviderIds;
import com.aipdf.assistant.ui.adapter.ModelAdapter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiSettingsActivity extends AppCompatActivity {

    private ActivityAiSettingsBinding binding;
    private ProviderManager providers;
    private SecureKeyStore keyStore;
    private ModelAdapter modelAdapter;

    private String currentProviderId = ProviderIds.GEMINI;
    private List<AiModel> allModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        providers = ProviderManager.getInstance(this);
        keyStore = SecureKeyStore.getInstance(this);
        currentProviderId = AppSettings.getDefaultProviderId();

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        modelAdapter = new ModelAdapter(model -> {
            binding.inputManualModel.setText(model.id);
            selectModel(model.id);
        });
        binding.modelsList.setLayoutManager(new LinearLayoutManager(this));
        binding.modelsList.setAdapter(modelAdapter);

        setupTabs();

        binding.btnValidate.setOnClickListener(v -> validateKey());
        binding.btnTest.setOnClickListener(v -> testConnection());
        binding.btnSaveKey.setOnClickListener(v -> saveKey());
        binding.btnRemoveKey.setOnClickListener(v -> removeKey());
        binding.btnFetchModels.setOnClickListener(v -> fetchModels());
        binding.btnSaveModel.setOnClickListener(v -> {
            String manual = binding.inputManualModel.getText() == null ? ""
                    : binding.inputManualModel.getText().toString().trim();
            if (manual.isEmpty()) {
                toast(getString(R.string.ai_settings_manual_model));
                return;
            }
            selectModel(manual);
        });
        binding.inputSearchModel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterModels(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.switchDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                providers.setActiveProviderId(currentProviderId);
                toast(getString(R.string.ai_settings_default_set,
                        ProviderIds.displayName(currentProviderId)));
            }
        });

        loadProviderState();
    }

    private void setupTabs() {
        binding.providerTabs.addTab(binding.providerTabs.newTab().setText(R.string.provider_gemini));
        binding.providerTabs.addTab(binding.providerTabs.newTab().setText(R.string.provider_openrouter));
        binding.providerTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentProviderId = tab.getPosition() == 1 ? ProviderIds.OPENROUTER : ProviderIds.GEMINI;
                loadProviderState();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        int initial = ProviderIds.OPENROUTER.equals(currentProviderId) ? 1 : 0;
        binding.providerTabs.getTabAt(initial).select();
    }

    private void loadProviderState() {
        AiProvider provider = providers.getProvider(currentProviderId);
        String masked = keyStore.maskedKey(currentProviderId);
        if (masked != null) {
            binding.inputKey.setText("");
            binding.inputKey.setHint(masked);
            binding.keyMetadata.setText(R.string.ai_settings_key_metadata);
        } else {
            binding.inputKey.setHint(R.string.ai_settings_api_key_hint);
            binding.keyMetadata.setText(R.string.ai_settings_no_key);
        }
        String model = provider.getModelId();
        binding.inputManualModel.setText(model == null ? "" : model);
        binding.switchDefault.setChecked(ProviderIds.GEMINI.equals(AppSettings.getDefaultProviderId())
                ? currentProviderId.equals(ProviderIds.GEMINI)
                : currentProviderId.equals(ProviderIds.OPENROUTER));
        binding.providerStatus.setText(getString(R.string.ai_provider_label,
                provider.getDisplayName(),
                TextUtils.isEmpty(model) ? getString(R.string.ai_settings_no_models) : model));

        List<AiModel> cached = providers.getCachedModels(currentProviderId);
        allModels = new ArrayList<>(cached);
        modelAdapter.submit(allModels);
        binding.modelsStatus.setText(cached.isEmpty() ? ""
                : getString(R.string.ai_settings_models_count, cached.size())
                    + " · " + getString(R.string.ai_settings_cached_models,
                        new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date())));
    }

    private String keyInput() {
        String k = binding.inputKey.getText() == null ? "" : binding.inputKey.getText().toString().trim();
        if (!k.isEmpty()) return k;
        return providers.getProvider(currentProviderId).getApiKey();
    }

    private void saveKey() {
        String key = binding.inputKey.getText() == null ? "" : binding.inputKey.getText().toString().trim();
        if (key.isEmpty()) {
            toast(getString(R.string.ai_settings_key_empty));
            return;
        }
        providers.getProvider(currentProviderId).setApiKey(key);
        binding.inputKey.setText("");
        toast(getString(R.string.ai_settings_key_saved));
        loadProviderState();
    }

    private void removeKey() {
        providers.getProvider(currentProviderId).clearApiKey();
        providers.getProvider(currentProviderId).setModelId(null);
        keyStore.removeApiKey(currentProviderId);
        toast(getString(R.string.ai_settings_key_removed));
        loadProviderState();
    }

    private void validateKey() {
        String key = keyInput();
        if (TextUtils.isEmpty(key)) {
            toast(getString(R.string.ai_settings_key_empty));
            return;
        }
        setBusy(true, getString(R.string.ai_settings_validating));
        providers.getProvider(currentProviderId).validateKey(key, new AiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setBusy(false, null);
                toast(getString(R.string.ai_settings_valid));
            }

            @Override
            public void onError(AiError error) {
                setBusy(false, null);
                toast(error.userMessage(AiSettingsActivity.this));
            }
        });
    }

    private void testConnection() {
        AiProvider provider = providers.getProvider(currentProviderId);
        String key = keyInput();
        if (TextUtils.isEmpty(key)) {
            toast(getString(R.string.ai_settings_key_empty));
            return;
        }
        if (TextUtils.isEmpty(provider.getModelId())) {
            toast(getString(R.string.ai_settings_manual_model));
            return;
        }
        setBusy(true, getString(R.string.ai_settings_validating));
        provider.chat(Collections.singletonList(
                ChatMessage.user("Reply with the single word: OK")), null, new AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                setBusy(false, null);
                toast(getString(R.string.ai_settings_connection_ok));
            }

            @Override
            public void onError(AiError error) {
                setBusy(false, null);
                toast(error.userMessage(AiSettingsActivity.this));
            }
        });
    }

    private void fetchModels() {
        AiProvider provider = providers.getProvider(currentProviderId);
        String key = keyInput();
        if (TextUtils.isEmpty(key)) {
            toast(getString(R.string.ai_settings_key_empty));
            return;
        }
        setBusy(true, getString(R.string.ai_settings_fetching));
        provider.listModels(key, new AiCallback<List<AiModel>>() {
            @Override
            public void onSuccess(List<AiModel> result) {
                setBusy(false, null);
                allModels = new ArrayList<>(result);
                providers.cacheModels(currentProviderId, result);
                modelAdapter.submit(allModels);
                binding.modelsStatus.setText(getString(R.string.ai_settings_models_count, result.size()));
            }

            @Override
            public void onError(AiError error) {
                setBusy(false, null);
                toast(error.userMessage(AiSettingsActivity.this));
            }
        });
    }

    private void filterModels(String query) {
        if (query.isEmpty()) {
            modelAdapter.submit(allModels);
            return;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        List<AiModel> filtered = new ArrayList<>();
        for (AiModel m : allModels) {
            if (m.id.toLowerCase(Locale.ROOT).contains(lower)
                    || m.displayName.toLowerCase(Locale.ROOT).contains(lower)) {
                filtered.add(m);
            }
        }
        modelAdapter.submit(filtered);
    }

    private void selectModel(String modelId) {
        if (TextUtils.isEmpty(modelId)) return;
        providers.getProvider(currentProviderId).setModelId(modelId);
        toast(getString(R.string.ai_settings_model_saved));
        binding.providerStatus.setText(getString(R.string.ai_provider_label,
                providers.getProvider(currentProviderId).getDisplayName(), modelId));
    }

    private void setBusy(boolean busy, String label) {
        binding.modelsProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (label != null) binding.modelsStatus.setText(label);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
