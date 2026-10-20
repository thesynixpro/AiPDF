package com.aipdf.assistant.pdf;

import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

public final class PageSizes {

    private PageSizes() {
    }

    public static PDRectangle resolve(String name, String orientation) {
        PDRectangle rect;
        if (name == null) rect = PDRectangle.A4;
        else switch (name.toUpperCase(java.util.Locale.US)) {
            case "LETTER":
                rect = PDRectangle.LETTER;
                break;
            case "LEGAL":
                rect = PDRectangle.LEGAL;
                break;
            case "A4":
            default:
                rect = PDRectangle.A4;
                break;
        }
        if (orientation != null && orientation.toLowerCase(java.util.Locale.US).contains("land")) {
            return new PDRectangle(rect.getHeight(), rect.getWidth());
        }
        return rect;
    }
}
