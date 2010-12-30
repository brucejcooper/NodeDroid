package com.eightbitcloud.internode;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

public class BrowserScraperActivity extends Activity {

    private WebView mWebView;
    EditText edittext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.browserscraper);
        
        
        

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new BrowserScraperWebViewClient());
        mWebView.loadUrl("http://www.google.com");

        
        
        edittext = (EditText) findViewById(R.id.urlEntry);
        
        Button goButton = (Button) findViewById(R.id.goButton);
        goButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                navigateToPage(edittext.getText().toString());
            }

        });

    }

    private void navigateToPage(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            edittext.setText(url);
            mWebView.loadUrl(url);
        } else {
            navigateToPage("http://" + url);
        }
        
    }

}
