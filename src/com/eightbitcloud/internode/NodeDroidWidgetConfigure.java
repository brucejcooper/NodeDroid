package com.eightbitcloud.internode;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RemoteViews;

public class NodeDroidWidgetConfigure extends Activity {
    int mAppWidgetId;
    Intent resultValue = new Intent();
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widgetconfig);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        Button saveButton = (Button) findViewById(R.id.configSave);
        saveButton.setOnClickListener(new OnClickListener() {
            
            public void onClick(View v) {
                updateWidget();
            }
        });

    }
    
    public void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.usagewidget);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);
        
        setResult(RESULT_OK, resultValue);
        finish();

    }
    
}
