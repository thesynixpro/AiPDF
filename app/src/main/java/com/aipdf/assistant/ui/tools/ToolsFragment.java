package com.aipdf.assistant.ui.tools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aipdf.assistant.R;
import com.aipdf.assistant.databinding.FragmentToolsBinding;
import com.aipdf.assistant.ui.adapter.ToolRowAdapter;
import com.aipdf.assistant.ui.home.ToolDef;
import com.aipdf.assistant.ui.home.ToolNavigator;

import java.util.ArrayList;
import java.util.List;

public class ToolsFragment extends Fragment {

    private FragmentToolsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentToolsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.toolList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.toolList.setAdapter(new ToolRowAdapter(buildTools(),
                tool -> ToolNavigator.open(requireContext(), tool)));

        binding.btnTextToPdf.setOnClickListener(v -> ToolNavigator.open(requireContext(), ToolNavigator.TOOL_TEXT_TO_PDF));
        binding.btnImagesToPdf.setOnClickListener(v -> ToolNavigator.open(requireContext(), ToolNavigator.TOOL_IMAGES_TO_PDF));
    }

    private List<ToolDef> buildTools() {
        List<ToolDef> tools = new ArrayList<>();
        tools.add(new ToolDef(ToolNavigator.TOOL_MERGE, getString(R.string.tool_merge),
                getString(R.string.tool_desc_merge), R.drawable.ic_merge));
        tools.add(new ToolDef(ToolNavigator.TOOL_SPLIT, getString(R.string.tool_split),
                getString(R.string.tool_desc_split), R.drawable.ic_split));
        tools.add(new ToolDef(ToolNavigator.TOOL_EXTRACT, getString(R.string.tool_extract),
                getString(R.string.tool_desc_extract), R.drawable.ic_page));
        tools.add(new ToolDef(ToolNavigator.TOOL_REORDER, getString(R.string.tool_reorder),
                getString(R.string.tool_desc_reorder), R.drawable.ic_sort));
        tools.add(new ToolDef(ToolNavigator.TOOL_ROTATE, getString(R.string.tool_rotate),
                getString(R.string.tool_desc_rotate), R.drawable.ic_rotate));
        tools.add(new ToolDef(ToolNavigator.TOOL_DELETE, getString(R.string.tool_delete_pages),
                getString(R.string.tool_desc_delete), R.drawable.ic_delete));
        tools.add(new ToolDef(ToolNavigator.TOOL_COMPRESS, getString(R.string.tool_compress),
                getString(R.string.tool_desc_compress), R.drawable.ic_compress));
        tools.add(new ToolDef(ToolNavigator.TOOL_WATERMARK, getString(R.string.tool_watermark),
                getString(R.string.tool_desc_watermark), R.drawable.ic_watermark));
        tools.add(new ToolDef(ToolNavigator.TOOL_PROTECT, getString(R.string.tool_protect),
                getString(R.string.tool_desc_protect), R.drawable.ic_lock));
        tools.add(new ToolDef(ToolNavigator.TOOL_UNPROTECT, getString(R.string.tool_unprotect),
                getString(R.string.tool_desc_unprotect), R.drawable.ic_unlock));
        tools.add(new ToolDef(ToolNavigator.TOOL_EXTRACT_TEXT, getString(R.string.tool_extract_text),
                getString(R.string.tool_desc_extract_text), R.drawable.ic_extract_text));
        tools.add(new ToolDef(ToolNavigator.TOOL_TEMPLATES, getString(R.string.templates_title),
                getString(R.string.home_templates), R.drawable.ic_template));
        return tools;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
