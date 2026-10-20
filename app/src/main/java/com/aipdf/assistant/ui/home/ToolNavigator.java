package com.aipdf.assistant.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aipdf.assistant.ui.TextToPdfActivity;
import com.aipdf.assistant.ui.templates.TemplateGalleryActivity;
import com.aipdf.assistant.ui.tools.PdfToolActivity;

/** Maps tool identifiers to their target screens. */
public final class ToolNavigator {

    public static final String TOOL_TEXT_TO_PDF = "text_to_pdf";
    public static final String TOOL_IMAGES_TO_PDF = "images_to_pdf";
    public static final String TOOL_MERGE = "merge";
    public static final String TOOL_SPLIT = "split";
    public static final String TOOL_EXTRACT = "extract";
    public static final String TOOL_REORDER = "reorder";
    public static final String TOOL_ROTATE = "rotate";
    public static final String TOOL_DELETE = "delete_pages";
    public static final String TOOL_COMPRESS = "compress";
    public static final String TOOL_WATERMARK = "watermark";
    public static final String TOOL_PROTECT = "protect";
    public static final String TOOL_UNPROTECT = "unprotect";
    public static final String TOOL_EXTRACT_TEXT = "extract_text";
    public static final String TOOL_TEMPLATES = "templates";

    private ToolNavigator() {
    }

    public static void open(Context context, String tool) {
        if (TOOL_TEXT_TO_PDF.equals(tool)) {
            context.startActivity(new Intent(context, TextToPdfActivity.class));
            return;
        }
        if (TOOL_TEMPLATES.equals(tool)) {
            context.startActivity(new Intent(context, TemplateGalleryActivity.class));
            return;
        }
        Intent intent = PdfToolActivity.intentFor(context, tool);
        context.startActivity(intent);
    }
}
