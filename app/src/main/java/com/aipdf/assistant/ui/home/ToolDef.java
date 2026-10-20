package com.aipdf.assistant.ui.home;

/** Immutable descriptor for a PDF tool entry. */
public class ToolDef {
    public final String id;
    public final String name;
    public final String description;
    public final int iconRes;

    public ToolDef(String id, String name, String description, int iconRes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconRes = iconRes;
    }
}
