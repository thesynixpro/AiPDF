package com.aipdf.assistant.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aipdf.assistant.R;
import com.aipdf.assistant.settings.AppSettings;
import com.aipdf.assistant.ui.chat.AssistantFragment;
import com.aipdf.assistant.ui.files.FilesFragment;
import com.aipdf.assistant.ui.home.HomeFragment;
import com.aipdf.assistant.ui.settings.SettingsFragment;
import com.aipdf.assistant.ui.tools.ToolsFragment;
import com.aipdf.assistant.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyThemeOnStart();
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return switchTo(new HomeFragment());
            if (id == R.id.nav_assistant) return switchTo(new AssistantFragment());
            if (id == R.id.nav_files) return switchTo(new FilesFragment());
            if (id == R.id.nav_tools) return switchTo(new ToolsFragment());
            if (id == R.id.nav_settings) return switchTo(new SettingsFragment());
            return false;
        });

        handleViewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleViewIntent(intent);
    }

    private void handleViewIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        Uri data = intent.getData();
        if (data != null) {
            com.aipdf.assistant.util.PendingIntentBus.setPendingImportUri(data);
            binding.bottomNav.setSelectedItemId(R.id.nav_files);
        }
    }

    private boolean switchTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host, fragment)
                .commit();
        return true;
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host, fragment)
                .commit();
    }

    public void openAssistant() {
        binding.bottomNav.setSelectedItemId(R.id.nav_assistant);
    }

    public void openFiles() {
        binding.bottomNav.setSelectedItemId(R.id.nav_files);
    }
}
