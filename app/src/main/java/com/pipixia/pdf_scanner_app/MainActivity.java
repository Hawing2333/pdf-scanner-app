package com.pipixia.pdf_scanner_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tvFileName, tvPageInfo, tvProgress, tvComplete, tvHint, tvResultSummary;
    private MaterialButton btnSelect, btnOcr, btnSearch, btnViewPdf;
    private EditText etKeyword;
    private MaterialCheckBox cbCase, cbRegex;
    private LinearProgressIndicator progressBar;
    private LinearLayout llProgress;
    private RecyclerView rvResults;
    private View cardSearch;

    private ResultAdapter adapter;
    private PdfHelper pdfHelper;
    private Uri currentUri;
    private int pageCount = 0;
    private final Map<Integer, String> pageTexts = new TreeMap<>();
    private boolean isProcessing = false;

    private final ActivityResultLauncher<String[]> pickPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) loadPdf(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupStatusBar();
        initViews();
        setupListeners();
    }

    private void setupStatusBar() {
        // 状态栏颜色与工具栏一致（primary_dark），图标为白色
        getWindow().setStatusBarColor(getColor(R.color.primary_dark));
    }

    private void initViews() {
        tvFileName = findViewById(R.id.tv_file_name);
        tvPageInfo = findViewById(R.id.tv_page_info);
        tvProgress = findViewById(R.id.tv_progress);
        tvComplete = findViewById(R.id.tv_complete);
        tvHint = findViewById(R.id.tv_hint);
        tvResultSummary = findViewById(R.id.tv_result_summary);

        btnSelect = findViewById(R.id.btn_select);
        btnOcr = findViewById(R.id.btn_ocr);
        btnSearch = findViewById(R.id.btn_search);
        btnViewPdf = findViewById(R.id.btn_view_pdf);

        etKeyword = findViewById(R.id.et_keyword);
        cbCase = findViewById(R.id.cb_case);
        cbRegex = findViewById(R.id.cb_regex);

        progressBar = findViewById(R.id.progress_bar);
        llProgress = findViewById(R.id.ll_progress);
        cardSearch = findViewById(R.id.card_search);
        rvResults = findViewById(R.id.rv_results);

        adapter = new ResultAdapter();
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        adapter.setListener(pageNumber -> openPdfViewer(pageNumber - 1));
    }

    private void setupListeners() {
        btnSelect.setOnClickListener(v -> pickPdf.launch(new String[]{"application/pdf"}));

        btnOcr.setOnClickListener(v -> startOcr());

        btnSearch.setOnClickListener(v -> doSearch());

        etKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });

        btnViewPdf.setOnClickListener(v -> openPdfViewer(0));
    }

    private void loadPdf(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            currentUri = uri;

            if (pdfHelper != null) pdfHelper.close();
            pdfHelper = new PdfHelper(this, uri);
            pageCount = pdfHelper.getPageCount();

            // 获取文件名
            String fileName = "PDF";
            try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex("_display_name");
                    if (idx >= 0) fileName = cursor.getString(idx);
                }
            }

            tvFileName.setText(fileName);
            tvFileName.setTextColor(getColor(R.color.text_dark));
            tvPageInfo.setVisibility(View.VISIBLE);
            tvPageInfo.setText(pageCount + " 页");

            btnOcr.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.GONE);
            tvComplete.setVisibility(View.GONE);
            llProgress.setVisibility(View.GONE);
            cardSearch.setVisibility(View.GONE);
            tvResultSummary.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
            btnViewPdf.setVisibility(View.GONE);

            pageTexts.clear();
            adapter.setItems(new ArrayList<>());

        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("错误")
                    .setMessage("加载PDF失败: " + e.getMessage())
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private void startOcr() {
        if (currentUri == null || isProcessing) return;

        isProcessing = true;
        llProgress.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        tvComplete.setVisibility(View.GONE);
        cardSearch.setVisibility(View.GONE);
        tvResultSummary.setVisibility(View.GONE);
        rvResults.setVisibility(View.GONE);
        btnViewPdf.setVisibility(View.GONE);
        pageTexts.clear();
        btnOcr.setEnabled(false);

        new Thread(() -> {
            try {
                if (pdfHelper == null) {
                    pdfHelper = new PdfHelper(this, currentUri);
                }
                int total = pdfHelper.getPageCount();

                for (int i = 0; i < total; i++) {
                    final int idx = i;
                    runOnUiThread(() -> {
                        tvProgress.setText("正在识别第 " + (idx + 1) + "/" + total + " 页...");
                        progressBar.setProgress((int) ((idx + 1) * 100f / total));
                    });

                    String text = pdfHelper.recognizePage(i);
                    pageTexts.put(i, text);
                }

                runOnUiThread(() -> {
                    isProcessing = false;
                    llProgress.setVisibility(View.GONE);
                    tvComplete.setVisibility(View.VISIBLE);
                    tvComplete.setText("识别完成！共 " + total + " 页");
                    cardSearch.setVisibility(View.VISIBLE);
                    tvHint.setText("请输入关键词进行搜索");
                    tvHint.setVisibility(View.VISIBLE);
                    rvResults.setVisibility(View.GONE);
                    btnViewPdf.setVisibility(View.VISIBLE);
                    btnOcr.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    llProgress.setVisibility(View.GONE);
                    btnOcr.setEnabled(true);
                    new AlertDialog.Builder(this)
                            .setTitle("错误")
                            .setMessage("OCR识别失败: " + e.getMessage())
                            .setPositiveButton("确定", null)
                            .show();
                });
            }
        }).start();
    }

    private void doSearch() {
        String keyword = etKeyword.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pageTexts.isEmpty()) {
            Toast.makeText(this, "请先识别PDF文件", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SearchResult> results = new ArrayList<>();
        boolean caseSensitive = cbCase.isChecked();
        boolean useRegex = cbRegex.isChecked();

        for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
            int pageNum = entry.getKey();
            String text = entry.getValue();

            if (useRegex) {
                try {
                    int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                    Pattern pattern = Pattern.compile(keyword, flags);
                    java.util.regex.Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        String context = getContext(text, matcher.start(), matcher.end());
                        results.add(new SearchResult(pageNum + 1, context));
                    }
                } catch (Exception ignored) {}
            } else {
                String searchText = caseSensitive ? text : text.toLowerCase();
                String searchKw = caseSensitive ? keyword : keyword.toLowerCase();
                int start = 0;
                while (true) {
                    int pos = searchText.indexOf(searchKw, start);
                    if (pos == -1) break;
                    String context = getContext(text, pos, pos + keyword.length());
                    results.add(new SearchResult(pageNum + 1, context));
                    start = pos + keyword.length();
                }
            }
        }

        if (results.isEmpty()) {
            tvResultSummary.setVisibility(View.VISIBLE);
            tvResultSummary.setText("未找到「" + keyword + "」");
            rvResults.setVisibility(View.GONE);
            tvHint.setVisibility(View.GONE);
            return;
        }

        // 统计每页匹配数
        StringBuilder summary = new StringBuilder("找到 " + results.size() + " 处匹配: ");
        java.util.Map<Integer, Integer> pageCounts = new java.util.TreeMap<>();
        for (SearchResult r : results) {
            pageCounts.put(r.pageNumber, pageCounts.getOrDefault(r.pageNumber, 0) + 1);
        }
        boolean first = true;
        for (Map.Entry<Integer, Integer> e : pageCounts.entrySet()) {
            if (!first) summary.append(", ");
            summary.append("第").append(e.getKey()).append("页(").append(e.getValue()).append("次)");
            first = false;
        }

        tvResultSummary.setVisibility(View.VISIBLE);
        tvResultSummary.setText(summary.toString());
        rvResults.setVisibility(View.VISIBLE);
        tvHint.setVisibility(View.GONE);
        adapter.setItems(results);
    }

    private String getContext(String text, int start, int end) {
        int window = 40;
        int ctxStart = Math.max(0, start - window);
        int ctxEnd = Math.min(text.length(), end + window);
        String prefix = text.substring(ctxStart, start).replace("\n", " ").trim();
        String suffix = text.substring(end, ctxEnd).replace("\n", " ").trim();
        String matched = text.substring(start, end).replace("\n", " ").trim();
        return "..." + prefix + "[" + matched + "]" + suffix + "...";
    }

    private void openPdfViewer(int pageIndex) {
        if (currentUri == null) return;
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.setData(currentUri);
        intent.putExtra("page", pageIndex);
        intent.putExtra("total", pageCount);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfHelper != null) {
            try { pdfHelper.close(); } catch (Exception ignored) {}
        }
        OcrHelper.release();
    }
}
