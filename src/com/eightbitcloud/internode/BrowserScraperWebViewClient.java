package com.eightbitcloud.internode;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserScraperWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

}
