package com.example.frontend.ui.library;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.frontend.R;

import java.net.URLEncoder;

public class ViewDocumentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_document);

        WebView webView = findViewById(R.id.webView);
        ProgressBar progressBar = findViewById(R.id.webProgressBar);

        // 1. Lấy URL từ Intent
        String fileUrl = getIntent().getStringExtra("FILE_URL");

        webView.clearCache(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

// Giả lập trình duyệt Desktop để Google Docs ưu tiên load nhanh hơn
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

// 2. Encode URL và dùng GVIEW bản minimal
        try {
            String encodedUrl = java.net.URLEncoder.encode(fileUrl, "UTF-8");
            // Thêm &rm=minimal để Google Docs load nhẹ hơn, tránh bị timeout lần đầu
            String finalUrl = "https://docs.google.com/gview?embedded=true&url=" + encodedUrl + "&rm=minimal";
            webView.loadUrl(finalUrl);
        } catch (Exception e) {
            webView.loadUrl("https://docs.google.com/gview?embedded=true&url=" + fileUrl);
        }
    }

    // Xử lý nút Back của điện thoại để quay lại trang trước trong WebView nếu có
    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webView);
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}