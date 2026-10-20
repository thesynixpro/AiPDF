package com.aipdf.assistant.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * Thin wrapper over Android's native {@link PdfRenderer} for on-screen display.
 * Keeps a single page open at a time to limit memory use on low-RAM devices.
 */
public class PdfPageRenderer implements AutoCloseable {

    private final ParcelFileDescriptor descriptor;
    private final PdfRenderer renderer;

    public PdfPageRenderer(File file) throws IOException {
        descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        renderer = new PdfRenderer(descriptor);
    }

    public int pageCount() {
        return renderer.getPageCount();
    }

    /**
     * Renders page (0-based) scaled so the width matches targetWidthPx.
     * Caller owns the returned bitmap and should recycle it.
     */
    public Bitmap renderPage(int index, int targetWidthPx) {
        if (index < 0 || index >= pageCount()) return null;
        try (PdfRenderer.Page page = renderer.openPage(index)) {
            int width = Math.max(1, targetWidthPx);
            int height = (int) ((float) page.getHeight() / page.getWidth() * width);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(android.graphics.Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    public float aspectRatio(int index) {
        if (index < 0 || index >= pageCount()) return 1.414f;
        try (PdfRenderer.Page page = renderer.openPage(index)) {
            return (float) page.getHeight() / page.getWidth();
        }
    }

    @Override
    public void close() {
        try {
            renderer.close();
        } catch (Exception ignored) {
        }
        try {
            descriptor.close();
        } catch (Exception ignored) {
        }
    }
}
