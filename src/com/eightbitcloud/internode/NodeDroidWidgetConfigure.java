package com.eightbitcloud.internode;

import java.util.Collections;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.eightbitcloud.internode.data.AccountProvider;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Service;

public class NodeDroidWidgetConfigure extends PreferenceActivity {
    int mAppWidgetId;
    Intent resultValue = new Intent();
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        Log.d("Configure", "WidgetID is " + mAppWidgetId + ", Intent is " + intent);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        
        getPreferenceManager().setSharedPreferencesName("NodeDroidWidget" + mAppWidgetId);
        addPreferencesFromResource(R.xml.widget_config);
        
        
        ListPreference servicePref = (ListPreference) findPreference("service");
        
        Cursor servicesCursor = getContentResolver().query(AccountProvider.SERVICES_CONTENT_URI, null, null, null, null);
        String[] serviceNames = new String[servicesCursor.getCount()];
        String[] serviceValues = new String[servicesCursor.getCount()];
        int count = 0;
        if (servicesCursor.moveToFirst()) {
            do {
                Service srv = new Service(servicesCursor);
                serviceNames[count] = srv.getIdentifier().toString();
                serviceValues[count] = AccountProvider.SERVICES_CONTENT_URI.buildUpon().appendPath(Integer.toString(srv.getAccountID())).appendPath(srv.getIdentifier().getAccountNumber()).build().toString();
                count++;
            } while (servicesCursor.moveToNext());
        }
        servicesCursor.close();
        servicePref.setEntries(serviceNames);
        servicePref.setEntryValues(serviceValues);
        servicePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setMetricChoices(newValue.toString());
                return true;
            }
        });
        
        setMetricChoices(getPreferenceManager().getSharedPreferences().getString("service", null));
       
        
        Button saveButton = new Button(this);
        saveButton.setText(R.string.create_widget);
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                if (!prefs.contains("service")) {
                    Toast.makeText(NodeDroidWidgetConfigure.this, "Please select a service first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!prefs.contains("metric") || "".equals(prefs.getString("metric", ""))) {
                    Toast.makeText(NodeDroidWidgetConfigure.this, "Please select a metric first", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(NodeDroidWidgetConfigure.this);
                RemoteViews views = new RemoteViews(NodeDroidWidgetConfigure.class.getPackage().getName(), R.layout.usagewidget);
                AppWidgetProviderInfo widgetInfo = appWidgetManager.getAppWidgetInfo(mAppWidgetId);
                NodeDroidWidgetProvider.updateWidget(NodeDroidWidgetConfigure.this, views, mAppWidgetId, widgetInfo);
                appWidgetManager.updateAppWidget(mAppWidgetId, views);

                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        getListView().addFooterView(saveButton);

    }


    protected void setMetricChoices(String newServiceUri) {
        List<MetricGroup> mgl = Collections.emptyList();
        
        if (newServiceUri != null) {
            Cursor serviceCursor = getContentResolver().query(Uri.parse(newServiceUri), null, null, null, null);
            if (serviceCursor.moveToFirst()) {
                Service srv = new Service(serviceCursor);
                mgl = srv.getAllMetricGroups();
            } else {
                Toast.makeText(this, "No service at " + newServiceUri, Toast.LENGTH_LONG).show();
            }
            serviceCursor.close();
        }
        ListPreference metricPref = (ListPreference) findPreference("metric");
        String[] mgs = new String[mgl.size()];
        for (int i = 0; i < mgs.length; i++) {
            mgs[i] = mgl.get(i).getName();
        }
        metricPref.setEntries(mgs);
        metricPref.setEntryValues(mgs);

        
    }
    
//    public void updateWidget() {
//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//        
//        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.usagewidget);
//        appWidgetManager.updateAppWidget(mAppWidgetId, views);
//        
//        setResult(RESULT_OK, resultValue);
//        finish();
//
//    }
    
}
