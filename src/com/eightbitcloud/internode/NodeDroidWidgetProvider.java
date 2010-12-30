package com.eightbitcloud.internode;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.RemoteViews;

public class NodeDroidWidgetProvider  extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, NodeUsage.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            

            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.usagewidget);
            views.setOnClickPendingIntent(R.id.widgetImage, pendingIntent);
            views.setImageViewBitmap(R.id.widgetImage,  createGraph(context));
            
            

            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
    
    public Bitmap createGraph(Context mContext) {
        Bitmap bm = Bitmap.createBitmap(294, 72, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStrokeWidth(5.0f);
        canvas.drawLine(0, 0, 300,100, p);
        canvas.drawRect(0,0, canvas.getWidth()-1, canvas.getHeight()-1, p);
        
        return bm;

    }
}
