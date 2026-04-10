package com.pipixia.pdf_scanner_app;

public class SearchResult {
    public final int pageNumber;
    public final String context;

    public SearchResult(int pageNumber, String context) {
        this.pageNumber = pageNumber;
        this.context = context;
    }
}
