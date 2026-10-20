package com.aipdf.assistant.util;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Locale;

public final class FileUtils {

    private FileUtils() {
    }

    public static String humanSize(long bytes) {
        if (bytes < 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        double size = bytes;
        int unit = 0;
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(size) + " " + units[unit];
    }

    public static String displayName(Context context, Uri uri) {
        if (uri == null) return "document";
        String name = null;
        try (android.database.Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        if (name == null) name = "document";
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) name = name + ".pdf";
        return sanitizeFileName(name);
    }

    public static String queryDisplayName(Context context, Uri uri, String fallback) {
        try (android.database.Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && !c.isNull(idx)) return c.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return "document.pdf";
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_").trim();
        if (cleaned.isEmpty()) cleaned = "document.pdf";
        if (!cleaned.toLowerCase(Locale.ROOT).endsWith(".pdf")) cleaned += ".pdf";
        return cleaned;
    }

    public static File documentsDir(Context context) {
        File dir = new File(context.getFilesDir(), "documents");
        if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        return dir;
    }

    public static File cacheDir(Context context) {
        File dir = new File(context.getCacheDir(), "tmp");
        if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        return dir;
    }

    public static File uniqueFile(File dir, String name) {
        File out = new File(dir, name);
        if (!out.exists()) return out;
        String base = name.endsWith(".pdf") ? name.substring(0, name.length() - 4) : name;
        String ext = name.endsWith(".pdf") ? ".pdf" : "";
        for (int i = 1; ; i++) {
            File candidate = new File(dir, base + " (" + i + ")" + ext);
            if (!candidate.exists()) return candidate;
        }
    }

    public static long copy(InputStream in, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        long total = 0;
        try (OutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[32 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                total += n;
            }
        }
        return total;
    }

    public static boolean importUriToDocuments(Context context, Uri uri, File dest) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open source stream");
            copy(in, dest);
            return true;
        }
    }

    public static String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isImageMime(String mime) {
        return mime != null && mime.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    public static String mimeOf(File file) {
        String ext = extensionOf(file.getName());
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime != null ? mime : "application/octet-stream";
    }
}
