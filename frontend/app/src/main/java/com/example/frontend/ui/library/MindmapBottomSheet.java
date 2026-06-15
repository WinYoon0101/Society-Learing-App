package com.example.frontend.ui.library;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.frontend.R;
import com.example.frontend.data.model.MindmapData;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MindmapBottomSheet extends BottomSheetDialogFragment {

    private final MindmapData data;

    public MindmapBottomSheet(MindmapData data) {
        this.data = data;
    }

    // --- Ép Bottom Sheet mở rộng hết cỡ để xem cho đã ---
    @Override
    public void onStart() {
        super.onStart();
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_mindmap_bottom_sheet, container, false);

        TextView tvTopic = view.findViewById(R.id.tvMindmapTopic);
        TextView tvSummary = view.findViewById(R.id.tvMindmapSummary);
        WebView webViewMindmap = view.findViewById(R.id.webViewMindmap);

        if (data != null) {
            tvTopic.setText(data.getTopic() != null ? data.getTopic() : "Sơ đồ tư duy");
            tvSummary.setText(data.getSummary() != null ? data.getSummary() : "");

            WebSettings webSettings = webViewMindmap.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);

            // Xử lý bắt Log lỗi từ JavaScript để debug nếu trắng màn hình
            webViewMindmap.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d("WebView_JS", consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                    return super.onConsoleMessage(consoleMessage);
                }
            });
            webViewMindmap.setWebViewClient(new WebViewClient());

            // Quét dữ liệu ra Markdown
            String markdownText = generateMarkdownFromData(data);

            // Lấy HTML
            String htmlData = getHtmlTemplate(markdownText);

            // Tải lên WebView
            webViewMindmap.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlData, "text/html", "UTF-8", null);
        }

        return view;
    }

    // --- HÀM TẠO MARKDOWN ---
    private String generateMarkdownFromData(MindmapData data) {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(data.getTopic() != null ? data.getTopic() : "").append("\n");

        if (data.getNodes() != null) {
            for (MindmapData.Node node : data.getNodes()) {
                md.append("## ").append(node.getTitle() != null ? node.getTitle() : "").append("\n");

                if (node.getDetails() != null && !node.getDetails().isEmpty()) {
                    md.append("- ").append(node.getDetails()).append("\n");
                }

                if (node.getSubNodes() != null) {
                    for (MindmapData.SubNode sub : node.getSubNodes()) {
                        md.append("### ").append(sub.getTitle() != null ? sub.getTitle() : "").append("\n");

                        if (sub.getDetails() != null && !sub.getDetails().isEmpty()) {
                            md.append("- ").append(sub.getDetails()).append("\n");
                        }
                    }
                }
            }
        }
        return md.toString();
    }

    // --- TEMPLATE GIẤU DỮ LIỆU BẰNG <PRE> SIÊU AN TOÀN ---
    private String getHtmlTemplate(String markdownContent) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">\n" +
                "    <style>\n" +
                "        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #ffffff; }\n" +
                "        #mindmap { width: 100vw; height: 100vh; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "    <pre id=\"md-content\" style=\"display:none;\">\n" + markdownContent + "\n</pre>\n" +
                "    <svg id=\"mindmap\"></svg>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/d3@7\"></script>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/markmap-view\"></script>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/markmap-lib\"></script>\n" +
                "    <script>\n" +
                "        console.log('Bắt đầu khởi tạo Markmap...');\n" +
                "        setTimeout(function() {\n" +
                "           try {\n" +
                "               const { Transformer, Markmap } = window.markmap;\n" +
                "               const markdown = document.getElementById('md-content').textContent;\n" +
                "               console.log('Markdown đã lấy thành công!');\n" +
                "               \n" +
                "               const transformer = new Transformer();\n" +
                "               const { root } = transformer.transform(markdown);\n" +
                "               \n" +
                "               Markmap.create('#mindmap', {\n" +
                "                  color: () => '#10B981', \n" +
                "                  initialExpandLevel: 3,  \n" +
                "                  maxWidth: 250,          \n" +
                "                  spacingHorizontal: 80,  \n" +
                "                  spacingVertical: 10     \n" +
                "               }, root);\n" +
                "               console.log('Vẽ đồ thị thành công!');\n" +
                "           } catch (e) {\n" +
                "               console.error('Lỗi khi vẽ Mindmap: ' + e.message);\n" +
                "           }\n" +
                "        }, 500); // Đợi 0.5s để thư viện D3 tải xong từ mạng\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}