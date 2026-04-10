package com.pipixia.pdf_scanner_app;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class PdfViewerActivity extends AppCompatActivity {

    private RecyclerView rvPages;
    private TextView tvTitle;
    private EditText etGotoPage;
    private PdfHelper pdfHelper;
    private int currentPage;
    private int totalPages;
    private LinearLayoutManager lm;

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
        etGotoPage = findViewById(R.id.et_goto_page);
        MaterialButton btnGoto = findViewById(R.id.btn_goto);

        rvPages = findViewById(R.id.rv_pages);
        lm = new LinearLayoutManager(this);
        rvPages.setLayoutManager(lm);
        PageAdapter adapter = new PageAdapter();
        rvPages.setAdapter(adapter);

        updateTitle();

        // 跳转到目标页（延迟确保RecyclerView已布局完成）
        rvPages.postDelayed(() -> {
            lm.scrollToPositionWithOffset(currentPage, 0);
            updateTitle();
        }, 300);

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

        // 导航按钮
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton btnNext = findViewById(R.id.btn_next);

        btnBack.setOnClickListener(v -> finish());
        btnPrev.setOnClickListener(v -> goToPage(currentPage - 1));
        btnNext.setOnClickListener(v -> goToPage(currentPage + 1));

        // 跳转指定页
        btnGoto.setOnClickListener(v -> doGoToPage());
        etGotoPage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doGoToPage();
                return true;
            }
            return false;
        });
    }

    private void goToPage(int page) {
        if (page >= 0 && page < totalPages) {
            currentPage = page;
            lm.scrollToPositionWithOffset(currentPage, 0);
            updateTitle();
        }
    }

    private void doGoToPage() {
        String text = etGotoPage.getText().toString().trim();
        if (text.isEmpty()) return;
        try {
            int page = Integer.parseInt(text);
            if (page >= 1 && page <= totalPages) {
                goToPage(page - 1);
                etGotoPage.setText("");
            } else {
                Toast.makeText(this, "页码超出范围 (1-" + totalPages + ")", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的页码", Toast.LENGTH_SHORT).show();
        }
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
