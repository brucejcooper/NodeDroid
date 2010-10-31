package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.eightbitcloud.internode.data.MeasuredValue;
import com.eightbitcloud.internode.data.Value;

public class QuotaBarGraph extends View {

    float time = 0.55f;
    
    Drawable clockBitmap;
    
    private static final float ARROW_HEIGHT = 5;
    private static final float ARROW_HALF_WIDTH = 4;
    
    GraphColors graphColors = new GraphColors(0xff43be6d, 0xFF166d6e, 0xFFf47836);
    private Paint timePaint;
    private List<MeasuredValue> values;
    private Value maximum;

    
    /**
     * Constructor. This version is only needed if you will be instantiating the
     * object manually (not from a layout XML file).
     * 
     * @param context
     */
    public QuotaBarGraph(Context context) {
        super(context);
        setPadding(3, 8, 8, 3);

    }
    
    
    public void setUsage(List<MeasuredValue> values, Value maximum) {
        this.values = new ArrayList<MeasuredValue>(values);
        Collections.sort(this.values, Collections.reverseOrder());
        this.maximum = maximum;
        invalidate();
    }
    
    public void setTime(float amt) {
        this.time = amt;
        invalidate();
    }
    
    
    public Paint getTimePaint() {
        if (timePaint == null) {
            timePaint = new Paint();
            timePaint.setAntiAlias(true);
//            timePaint.setStyle(Style.STROKE);
            timePaint.setColor(0xFF000000);

        }
        return timePaint;
    }

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context,
     *      android.util.AttributeSet)
     */
    public QuotaBarGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPadding(3, 8, 8, 3);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UsageGraphView);

        CharSequence s = a.getString(R.styleable.UsageGraphView_txt);
        if (s != null) {
//            setText(s.toString());
        }
        a.recycle();
        
        Resources res = context.getResources();
        clockBitmap = res.getDrawable(R.drawable.clock);
    }
    
    public void setGraphColors(GraphColors colors) {
        this.graphColors = colors;
    }


    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * 
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = 20; // TODO FIXME(int) mTextPaint.measureText(mText) + getPaddingLeft() + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    /**
     * Determines the height of this view
     * 
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = 50; //(int) (-mAscent + mTextPaint.descent()) + getPaddingTop() + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Render the text
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        

       RectF r = new RectF(getPaddingLeft(), getPaddingTop(), getWidth()-getPaddingRight(), 30);
       
       canvas.drawRect(r.left+2, r.top+2, r.right+2, r.bottom+2, graphColors.dropShadowPaint);
       canvas.drawRoundRect(r, 3, 3, graphColors.barBackgroundPaint);
       
       long sum = 0;
       float lastStop = 0.0f;
       int paintIndex = 0;
       
       if (maximum.getAmt() > 0) { // Prevent DIV0
           for (MeasuredValue mv: values) {
               sum += mv.getAmount().getAmt();
               float thisStop = sum / (float) maximum.getAmt();
               
               if ((thisStop - lastStop)*r.width() > 0.5f) {
                   RectF usageRect = new RectF(r.left + r.width()*lastStop, r.top, r.left + r.width()*thisStop, r.bottom);
                   canvas.drawRect(usageRect, graphColors.barPaint[paintIndex]);
                   lastStop = thisStop;
               }
               paintIndex = (paintIndex + 1) % graphColors.barPaint.length;
           }
       }
       

       canvas.drawRoundRect(r, 3, 3, graphColors.graphBorderPaint);

       
       float timeX = getPaddingLeft() + r.width()*time;
       float top = r.top+0.75f;
       float bottom = r.bottom-0.75f;
       canvas.drawLines(new float[] { timeX-ARROW_HALF_WIDTH, top, 
                                      timeX+ARROW_HALF_WIDTH, top, 
                                      
                                      timeX+ARROW_HALF_WIDTH, top, 
                                      timeX, top+ARROW_HEIGHT, 
                                      
                                      timeX, top+ARROW_HEIGHT, 
                                      timeX-ARROW_HALF_WIDTH, top,

                                      
                                      timeX-ARROW_HALF_WIDTH, bottom, 
                                      timeX+ARROW_HALF_WIDTH, bottom, 
                                      
                                      timeX+ARROW_HALF_WIDTH, bottom, 
                                      timeX, bottom-ARROW_HEIGHT, 
                                      
                                      timeX, bottom-ARROW_HEIGHT, 
                                      timeX-ARROW_HALF_WIDTH, bottom,
                                      
                                      timeX, bottom,
                                      timeX, bottom+8
                                      
       
                                       }, getTimePaint());
       clockBitmap.setBounds((int)timeX-clockBitmap.getIntrinsicWidth()/2, (int)bottom+8, (int)timeX+clockBitmap.getIntrinsicWidth()/2, (int)bottom+8+clockBitmap.getIntrinsicHeight());
       
       clockBitmap.draw(canvas);
       


    }
    

}
