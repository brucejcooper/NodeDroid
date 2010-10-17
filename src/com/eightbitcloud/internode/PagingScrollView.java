package com.eightbitcloud.internode;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class PagingScrollView extends HorizontalScrollView {
    
    private LinearLayout contents;

    public PagingScrollView(Context ctx, AttributeSet attrs) 
    {
        super(ctx, attrs);
        contents = new LinearLayout(ctx);
        contents.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
        
        addView(contents);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        for (int i = 0; i < contents.getChildCount(); i++) {
            View child = contents.getChildAt(i);
            if (child.getLayoutParams().width != specSize) {
                child.setLayoutParams(new LinearLayout.LayoutParams(specSize, LayoutParams.FILL_PARENT));
            }

        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    
    
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
     }
        
    
    @Override
    protected float getLeftFadingEdgeStrength () {
        return 0.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength () {
        return 0.0f;
    }

    
    public void addPage(View child) {
        int width = getWidth();
        child.setLayoutParams(new LayoutParams(width, LayoutParams.FILL_PARENT));
        contents.addView(child);
        contents.requestLayout();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        boolean result =  super.onTouchEvent(evt);
        
        int width = getWidth();
        
        if (evt.getAction() == MotionEvent.ACTION_UP) {
            int pg = (getScrollX() + width/2) / width;
            smoothScrollTo(pg*width, 0);
        }
        
        return result;
    }

    public boolean hasPage(View v) {
        return contents.indexOfChild(v) != -1;
    }

    public void removePage(View v) {
        contents.removeView(v);
        
    }
    
    public int getPageCount() {
        return contents.getChildCount();
    }

    public void removeAllPages() {
        contents.removeAllViews();
        
    }

}
