package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.HorizontalScrollView;
import android.widget.ListAdapter;

@SuppressWarnings("deprecation")
public class PagingScrollView extends HorizontalScrollView {
    private AbsoluteLayout contents;
    private List<View> cachedViews = new ArrayList<View>();
    private View noViewsView;
    
    private ListAdapter adapter;
    private DataSetObserver observer = new AdapterObserver();

    public PagingScrollView(Context ctx, AttributeSet attrs) 
    {
        super(ctx, attrs);
        contents = new AbsoluteLayout(ctx);
        contents.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
        
        addView(contents);
    }
    
    public void setNoViewsView(View view) {
        noViewsView = view;
        layoutChildren();
    }
    
    public List<View> getPages() {
        return Collections.unmodifiableList(cachedViews);
    }
    
    
    public void setAdapter(ListAdapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterDataSetObserver(observer);
        }
        this.adapter = adapter;
        layoutChildren();
        
        if (adapter != null) {
            adapter.registerDataSetObserver(observer);
        }
        layoutChildren();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        for (int i = 0; i < contents.getChildCount(); i++) {
            View child = contents.getChildAt(i);
            if (child.getLayoutParams().width != specSize) {
                child.setLayoutParams(new AbsoluteLayout.LayoutParams(specSize, LayoutParams.FILL_PARENT, i * specSize, 0));
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
    
    protected void layoutChildren() {
        int width = getWidth();
        int height = getHeight();
        int count = adapter == null ? 0 :  adapter.getCount();
        boolean useNoViewsView = false;
        
        if (count == 0 && noViewsView != null) {
            count = 1;
            useNoViewsView=true;
        }
        
        LayoutParams params = new LayoutParams(Math.max(1, count)*width, height);
        contents.setLayoutParams(params);
        
        if (cachedViews == null || cachedViews.size() != count) {
            while (cachedViews.size() < count) {
                cachedViews.add(null);
            }
            while (cachedViews.size() < count) {
                cachedViews.remove(cachedViews.size()-1);
            }
        }
        
        contents.removeAllViews();
        
        if (useNoViewsView) {
            contents.addView(noViewsView);
            AbsoluteLayout.LayoutParams allp = new AbsoluteLayout.LayoutParams(width, height, 0, 0);
            noViewsView.setLayoutParams(allp);
        } else {
            for (int i = 0; i < count; i++) {
                View convertView = cachedViews.get(i);
                View view = adapter.getView(i, convertView, contents);
                cachedViews.set(i, view);
                contents.addView(view);
                AbsoluteLayout.LayoutParams allp = new AbsoluteLayout.LayoutParams(width, height, i*width, 0);
                view.setLayoutParams(allp);
            }
        }
        contents.requestLayout();
        
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        
        if (changed) {
            layoutChildren();
        }
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
    
    
    public class AdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            layoutChildren();
        }
        
        @Override
        public void onInvalidated() {
            super.onInvalidated();
            layoutChildren();
        }
    }

}
