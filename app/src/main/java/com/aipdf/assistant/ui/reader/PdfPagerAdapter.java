package com.aipdf.assistant.ui.reader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aipdf.assistant.R;
import com.aipdf.assistant.pdf.PdfPageRenderer;
import com.aipdf.assistant.util.AppExecutors;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.HashMap;
import java.util.Map;

/** Renders one PDF page per ViewPager2 item; only visible pages are rasterized. */
public class PdfPagerAdapter extends RecyclerView.Adapter<PdfPagerAdapter.VH> {

    private final PdfPageRenderer renderer;
    private final int pageCount;
    private final Map<Integer, android.graphics.Bitmap> cache = new HashMap<>();
    private final int[] screenWidth = {1080};
    private boolean darkMode;

    public PdfPagerAdapter(PdfPageRenderer renderer, int initialWidth) {
        this.renderer = renderer;
        this.pageCount = renderer.pageCount();
        this.screenWidth[0] = initialWidth;
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        clearCache();
        notifyDataSetChanged();
    }

    public void setWidth(int width) {
        if (width > 0 && Math.abs(width - screenWidth[0]) > 24) {
            screenWidth[0] = width;
            clearCache();
            notifyDataSetChanged();
        }
    }

    private void clearCache() {
        for (android.graphics.Bitmap bmp : cache.values()) {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
        cache.clear();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.image.setImageDrawable(null);
        holder.progress.setVisibility(View.VISIBLE);

        android.graphics.Bitmap cached = cache.get(position);
        if (cached != null && !cached.isRecycled()) {
            holder.image.setImageBitmap(cached);
            holder.progress.setVisibility(View.GONE);
            return;
        }

        final int width = screenWidth[0];
        final int index = position;
        AppExecutors.disk(() -> {
            android.graphics.Bitmap bmp = renderer.renderPage(index, width);
            if (bmp != null && darkMode) {
                bmp = invert(bmp);
            }
            final android.graphics.Bitmap result = bmp;
            AppExecutors.main(() -> {
                if (result != null) cache.put(index, result);
                if (holder.getAdapterPosition() == index || holder.getAdapterPosition() == RecyclerView.NO_POSITION) {
                    holder.progress.setVisibility(View.GONE);
                    if (result != null) holder.image.setImageBitmap(result);
                }
            });
        });
    }

    private android.graphics.Bitmap invert(android.graphics.Bitmap src) {
        android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
        matrix.set(new float[]{
                -1, 0, 0, 0, 255,
                0, -1, 0, 0, 255,
                0, 0, -1, 0, 255,
                0, 0, 0, 1, 0});
        android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(
                src.getWidth(), src.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(out);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(src, 0, 0, paint);
        return out;
    }

    @Override
    public int getItemCount() {
        return pageCount;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final CircularProgressIndicator progress;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.page_image);
            progress = itemView.findViewById(R.id.progress);
        }
    }
}
