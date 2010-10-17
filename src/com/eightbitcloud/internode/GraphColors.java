package com.eightbitcloud.internode;

import android.graphics.BlurMaskFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class GraphColors {
    
    Paint graphBorderPaint;
    Paint[] barPaint;
    Paint barBorderPaint;
    Paint mQuotaLinePaint;
    
    Paint dropShadowPaint;
    Paint barBackgroundPaint;
    
    
    public GraphColors(int barBorderColor, int graphBorderColor, int... barColor) {
        this.graphBorderPaint = new Paint();
        graphBorderPaint.setAntiAlias(true);
        graphBorderPaint.setStyle(Style.STROKE);
        graphBorderPaint.setStrokeWidth(1.5f);
        graphBorderPaint.setColor(graphBorderColor);
        
        mQuotaLinePaint = new Paint(graphBorderPaint);
        mQuotaLinePaint.setPathEffect(new DashPathEffect(new float[] {3, 3}, 0.0f));
        
        
        barPaint = new Paint[barColor.length];
        for (int i = 0; i < barColor.length; i++) {
            barPaint[i] = new Paint();
            barPaint[i].setColor(barColor[i]);
        }
        
        // TODO make this a recurring pattern
        barBackgroundPaint = new Paint();
        barBackgroundPaint.setColor(/*0xff74AC23*/ 0xffdddddd);

        dropShadowPaint = new Paint();
        dropShadowPaint.setColor(0xAf000000);
        dropShadowPaint.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
        
        barBorderPaint = new Paint();
        barBorderPaint.setColor(barBorderColor);
        barBorderPaint.setStyle(Style.STROKE);
        barBorderPaint.setAntiAlias(true);
        barBorderPaint.setStrokeWidth(1.5f);
    }
    
    
    
    public Paint getGraphBorderPaint() {
        return graphBorderPaint;
    }

    public void setGraphBorderPaint(Paint graphBorderPaint) {
        this.graphBorderPaint = graphBorderPaint;
    }

    public Paint[] getBarPaint() {
        return barPaint;
    }

    public void setBarPaint(Paint[] barPaint) {
        this.barPaint = barPaint;
    }

    public Paint getBarBorderPaint() {
        return barBorderPaint;
    }

    public void setBarBorderPaint(Paint barBorderPaint) {
        this.barBorderPaint = barBorderPaint;
    }

    public Paint getmQuotaLinePaint() {
        return mQuotaLinePaint;
    }

    public void setmQuotaLinePaint(Paint mQuotaLinePaint) {
        this.mQuotaLinePaint = mQuotaLinePaint;
    }

    public Paint getDropShadowPaint() {
        return dropShadowPaint;
    }

    public void setDropShadowPaint(Paint dropShadowPaint) {
        this.dropShadowPaint = dropShadowPaint;
    }

    public Paint getBarBackgroundPaint() {
        return barBackgroundPaint;
    }

    public void setBarBackgroundPaint(Paint barBackgroundPaint) {
        this.barBackgroundPaint = barBackgroundPaint;
    }


}
