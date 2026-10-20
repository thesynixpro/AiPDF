package com.aipdf.assistant.ui.templates;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.ai.models.DocumentSpec;
import com.aipdf.assistant.ai.models.DocumentSpecParser;
import com.aipdf.assistant.databinding.ActivityTemplateGalleryBinding;
import com.aipdf.assistant.pdf.DocumentTemplates;
import com.aipdf.assistant.ui.chat.ChatDetailActivity;
import com.aipdf.assistant.ui.chat.AssistantFragment;
import com.aipdf.assistant.ui.preview.DocumentPreviewActivity;
import com.aipdf.assistant.ui.adapter.TemplateAdapter;

public class TemplateGalleryActivity extends AppCompatActivity {

    private ActivityTemplateGalleryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTemplateGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        TemplateAdapter adapter = new TemplateAdapter(DocumentTemplates.all(), new TemplateAdapter.Listener() {
            @Override
            public void onUseTemplate(DocumentTemplates.Template template) {
                DocumentSpec spec = template.starterSpec();
                startActivity(DocumentPreviewActivity.intentFor(TemplateGalleryActivity.this,
                        DocumentSpecParser.toJson(spec)));
            }

            @Override
            public void onFillWithAi(DocumentTemplates.Template template) {
                AssistantFragment.startFromPrompt(template.aiPrompt);
                Intent intent = new Intent(TemplateGalleryActivity.this,
                        com.aipdf.assistant.ui.MainActivity.class);
                startActivity(intent);
            }
        });
        binding.templatesList.setLayoutManager(new LinearLayoutManager(this));
        binding.templatesList.setAdapter(adapter);
    }
}
