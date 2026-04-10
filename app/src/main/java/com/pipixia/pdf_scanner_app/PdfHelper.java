package com.pipixia.pdf_scanner_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

public class PdfHelper implements AutoCloseable {

    private final ParcelFileDescriptor pfd;
    private final PdfRenderer renderer;

    public PdfHelper(Context context, Uri uri) throws IOException {
        pfd = context.getContentResolver().openFileDescriptor(uri, "r");
        if (pfd == null) throw new IOException("无法打开文件");
        renderer = new PdfRenderer(pfd);
    }

    public int getPageCount() {
        return renderer.getPageCount();
    }

    public Bitmap renderPage(int index, int width) {
        PdfRenderer.Page page = renderer.openPage(index);
        float scale = (float) width / page.getWidth();
        int w = (int) (page.getWidth() * scale);
        int h = (int) (page.getHeight() * scale);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.WHITE);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bitmap;
    }

    public String recognizePage(int index) throws Exception {
        Bitmap bitmap = renderPage(index, 1080);
        try {
            return OcrHelper.recognize(bitmap);
        } finally {
            bitmap.recycle();
        }
    }

    @Override
    public void close() throws Exception {
        renderer.close();
        pfd.close();
    }
}
