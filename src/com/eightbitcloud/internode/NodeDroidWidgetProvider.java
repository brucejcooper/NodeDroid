package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.Collections;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.eightbitcloud.internode.data.AccountProvider;
import com.eightbitcloud.internode.data.MeasuredValue;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;

public class NodeDroidWidgetProvider  extends AppWidgetProvider {

    private static Typeface internodeFont;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        
        // Tell it to start updating.
        context.startService(new Intent(context, UsageUpdateService.class));
        
        // Perform this loop procedure for each App Widget that belongs to this provider
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            
            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.usagewidget);
            AppWidgetProviderInfo widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
            updateWidget(context, views, appWidgetId, widgetInfo);
            
            
            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    public static void updateWidgetsFor(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.usagewidget);
        
        for (int appWidgetId: appWidgetManager.getAppWidgetIds(new ComponentName(context, NodeDroidWidgetProvider.class))) {
            AppWidgetProviderInfo widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
            updateWidget(context, views, appWidgetId, widgetInfo);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    
    public static void updateWidget(Context context, RemoteViews views, int appWidgetId, AppWidgetProviderInfo widgetInfo) {
        Log.d("Widget", "Updating widget id " + appWidgetId + ", height is " + widgetInfo.minHeight);
        
        
        
        String headerText = "Error in configuration";
        
        SharedPreferences prefs = context.getSharedPreferences("NodeDroidWidget"+appWidgetId, Context.MODE_WORLD_READABLE);
        int textColor = Integer.parseInt(prefs.getString("textColour", "-1"));
        
        
        if (internodeFont == null) {
            internodeFont = Typeface.createFromAsset(context.getAssets(), "Arial Rounded Bold.ttf");
        }
       

        
        try {
            Uri uri = Uri.parse(prefs.getString("service", null));
            Cursor c = context.getContentResolver().query(uri, null, null, null, null);
            if (c.moveToFirst()) {
                Service service = new Service(c);
                Provider prov = ProviderStore.getInstance().getProvider(service.getIdentifier().getProvider()); 
                
                Drawable clockBitmap = context.getResources().getDrawable(R.drawable.clock);
                
                String[] mgNames = prefs.getString("metric", null).split(",");
                Paint paint = new Paint();
                paint.setTypeface(internodeFont);
                paint.setColor(textColor);
                paint.setTextSize(16.0f);
                
                Rect bounds = new Rect();
                int maxNameWidth = 0;
                int txtHeight = 0;
                for (String mgName: mgNames) {
                    String label = mgName + ": ";
                    paint.getTextBounds(label, 0, label.length(), bounds);
                    maxNameWidth = Math.max(maxNameWidth, bounds.width());
                    txtHeight = bounds.height();
                }

                Log.d("Widget", "Size is " + txtHeight);
                Bitmap bm = Bitmap.createBitmap(widgetInfo.minWidth, 45*mgNames.length, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bm);
                
                int graphHeight = bm.getHeight()/mgNames.length;

                for (String mgName: mgNames) {
                    MetricGroup mg = service.getMetricGroup(mgName);
                    
                    ArrayList<MeasuredValue> values = new ArrayList<MeasuredValue>(mg.getComponents());
                    Collections.sort(values, Collections.reverseOrder());
                    float time = service.getPlan() == null ? 0.0f : (float) service.getPlan().getPercentgeThroughMonth(System.currentTimeMillis());
            
                    canvas.drawText(mgName + ": ", 5, (graphHeight-20)/2 + txtHeight/2, paint);
                    
                    QuotaBarGraph.renderGraph(canvas, bm.getWidth(), graphHeight-20, 1, 10 + maxNameWidth, 1, 5, prov.getGraphColors(), values, mg.getAllocation(), time, clockBitmap);
                    canvas.translate(0, graphHeight);
                }
    
                headerText = service.getIdentifier().getAccountNumber();
                views.setImageViewBitmap(R.id.widgetImage,  bm);
            }
            c.close();
        } catch (NullPointerException ex) {
            // Probably caused by the preference not being present.
        }
        
        
        views.setTextViewText(R.id.widgetHeader, headerText);
        views.setTextColor(R.id.widgetHeader, textColor);
        
        String background = prefs.getString("background", "0");
        if ("0".equals(background)) {
            views.setInt(R.id.widgetBackground, "setBackgroundResource", 0);
        } else {
            int backID = context.getResources().getIdentifier(background, "drawable", context.getPackageName());
            views.setInt(R.id.widgetBackground, "setBackgroundResource", backID);
        }

        
        Intent showNodeDroidIntent = new Intent(context, NodeUsage.class);
        showNodeDroidIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
        // PendingIntents are treated as "cached copies" if they have the same ID.  Because we want different intents for different widgets,
        // We give the PendingIntent an ID based upon the appWidgetID.
        PendingIntent showNodeDroidPendingIntent = PendingIntent.getActivity(context, appWidgetId << 8 + 0, showNodeDroidIntent, 0);
        views.setOnClickPendingIntent(R.id.widgetImage, showNodeDroidPendingIntent);

        Log.d("Widget", "Setting extra " + AppWidgetManager.EXTRA_APPWIDGET_ID + " to " + appWidgetId);
        Intent configureWidgetIntent = new Intent(context, NodeDroidWidgetConfigure.class);
        configureWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId); 
        configureWidgetIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent configureWidgetIntentPendingIntent = PendingIntent.getActivity(context, appWidgetId << 8 + 1, configureWidgetIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_config, configureWidgetIntentPendingIntent);

        Intent refreshUsageIntent = new Intent(context, UsageUpdateService.class);
        PendingIntent refreshUsageIntentPendingIntent = PendingIntent.getService(context, appWidgetId << 8 + 2, refreshUsageIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_reload, refreshUsageIntentPendingIntent);

        
    }
}
