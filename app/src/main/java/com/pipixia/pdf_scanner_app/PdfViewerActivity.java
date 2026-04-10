package com.pipixia.pdf_scanner_app;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PdfViewerActivity extends AppCompatActivity {

    private RecyclerView rvPages;
    private TextView tvTitle;
    private PdfHelper pdfHelper;
    private int currentPage;
    private int totalPages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Uri uri = getIntent().getData();
        currentPage = getIntent().getIntExtra("page", 0);
        totalPages = getIntent().getIntExtra("total", 0);

        try {
            pdfHelper = new PdfHelper(this, uri);
            if (totalPages == 0) totalPages = pdfHelper.getPageCount();
        } catch (Exception e) {
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tv_viewer_title);
        updateTitle();

        rvPages = findViewById(R.id.rv_pages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        rvPages.setLayoutManager(lm);
        PageAdapter adapter = new PageAdapter();
        rvPages.setAdapter(adapter);

        // 跳转到目标页
        rvPages.post(() -> {
            lm.scrollToPositionWithOffset(currentPage, 0);
        });

        // 滚动监听，更新当前页
        rvPages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (firstVisible >= 0 && firstVisible != currentPage) {
                    currentPage = firstVisible;
                    updateTitle();
                }
            }
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton btnNext = findViewById(R.id.btn_next);

        btnBack.setOnClickListener(v -> finish());
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                lm.scrollToPositionWithOffset(currentPage, 0);
                updateTitle();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                lm.scrollToPositionWithOffset(currentPage, 0);
                updateTitle();
            }
        });
    }

    private void updateTitle() {
        tvTitle.setText("第 " + (currentPage + 1) + " / " + totalPages + " 页");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfHelper != null) {
            try { pdfHelper.close(); } catch (Exception ignored) {}
        }
    }

    // ─── 页面适配器 ───

    class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = new ImageView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ImageView iv = (ImageView) holder.itemView;
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setBackgroundColor(android.graphics.Color.WHITE);
            iv.setImageBitmap(null);

            new Thread(() -> {
                try {
                    Bitmap bmp = pdfHelper.renderPage(position, 1080);
                    runOnUiThread(() -> iv.setImageBitmap(bmp));
                } catch (Exception e) {
                    // 加载失败，显示空白
                }
            }).start();
        }

        @Override
        public int getItemCount() {
            return totalPages;
        }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }
}
