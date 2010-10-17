package com.eightbitcloud.internode;

import java.util.Collections;
import java.util.Iterator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.eightbitcloud.internode.data.Value;

public class GraphView extends View {

    private int mAscent;
    
    GraphColors graphColors = new GraphColors(0xff43be6d, 0xFF166d6e, 0xFFf47836);

    
    private Value maxValue;
    
    Value thresholdValue;
    public GraphData<? extends Object,Value> data = new GraphData<Object,Value>(Collections.emptyList(), Collections.EMPTY_MAP, GraphStyle.BAR, null);

    private Paint mTextPaint;

    /**
     * Constructor. This version is only needed if you will be instantiating the
     * object manually (not from a layout XML file).
     * 
     * @param context
     */
    public GraphView(Context context) {
        super(context);
        initLabelView();
    }
    
    
    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context,
     *      android.util.AttributeSet)
     */
    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLabelView();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UsageGraphView);

        CharSequence s = a.getString(R.styleable.UsageGraphView_txt);
        if (s != null) {
//            setText(s.toString());
        }
        a.recycle();

    }

    private final void initLabelView() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(10);
        mTextPaint.setColor(0xFFf47836);
//        
//        
//        
//        barPaint = new Paint();
//        barPaint.setColor(/*0xff74AC23*/ 0xff43be6d);
//
//        dropShadowPaint = new Paint();
//        dropShadowPaint.setColor(0xAf000000);
//        dropShadowPaint.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
//        
//        barBorderPaint = new Paint();
//        barBorderPaint.setColor(0xFF166d6e /*0xff4f7518*/);
//        barBorderPaint.setStyle(Style.STROKE);
//        barBorderPaint.setStrokeWidth(1.5f);
//        barBorderPaint.setAntiAlias(true);
        setPadding(3, 8, 8, 3);
    }
    
    public void setGraphColors(GraphColors colors) {
        this.graphColors = colors;
    }


    
    /**
     * Sets the text to display in this label
     * 
     * @param text
     *            The text to display. This will be drawn as one line.
     */
    public void setData(GraphData<? extends Object,Value> data) {
        this.data = data;
        maxValue = null;
        for (Value[] record: data.data.values()) {
            Value sum = sumValues(record);
            if (maxValue == null ||  sum.isGreaterThan(maxValue)) {
                maxValue = sum;
            }
        }
        
        requestLayout();
        invalidate();
    }
    
    

    private Value sumValues(Value[] record) {
        Value sum =new Value(0, record[0].getUnit());
        for (Value v: record) {
            if (v != null) {
                sum = sum.plus(v);
            }
        }
        return sum;
    }


    /**
     * Sets the text size for this label
     * 
     * @param size
     *            Font size
     */
    public void setTextSize(int size) {
        mTextPaint.setTextSize(size);
        requestLayout();
        invalidate();
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
//            result = (int) mTextPaint.measureText(mText) + getPaddingLeft() + getPaddingRight();
            result = 200;
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

        mAscent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = 200; // TODO make this a parameter....
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
        
        switch(data.style) {
        case BAR:
            drawBarGraph(canvas);
            break;
        case PIE:
            drawPieGraph(canvas);
            break;
        }
    }

    private void drawPieGraph(Canvas canvas) {
        int x1 = getPaddingLeft();
        int y1 = getPaddingTop();
        int x2 = getWidth()-getPaddingRight();
        int y2 = getHeight()-getPaddingBottom();
        int mx = (x1+x2)/2;
        int my = (y1+y2)/2;
        int size = Math.min(x2-x1, y2-y1);
        
        RectF oval = new RectF(x1,y1, x1+size, x1+size);
        
//        canvas.drawOval(oval, barPaint);
//        canvas.drawOval(oval, barBorderPaint);

        float oldpos = 0.0f;
        int paintIndex =0;
        float y = y1+20;
        for (Object key: data.xaxisData) {
            float pos =(float) (360*data.data.get(key)[0].divideByValue(maxValue));
            Paint c = graphColors.barPaint[paintIndex % graphColors.barPaint.length];
            
            canvas.drawArc(oval, oldpos, pos, true, c);
            canvas.drawArc(oval, oldpos, pos, true, graphColors.barBorderPaint);
            
            canvas.drawRect(x1+size+5, y-15, x1+size+20, y, c);
            canvas.drawRect(x1+size+5, y-15, x1+size+20, y, graphColors.barBorderPaint);
            
            canvas.drawText(data.formatter.format(key), x1+size+25, y, mTextPaint);
            y += 20;
            
            paintIndex++;
            oldpos += pos;
        }
        

        
    }
    
    private void drawBarGraph(Canvas canvas) {
//        Log.d(NodeUsage.TAG, "Drawing");
        
        int xAxisMargin = 45;
        int yAxisTopMargin = 0;
        int yAxisBottomMargin = 35;

        int x1 = getPaddingLeft() + xAxisMargin;
        int y1 = getPaddingTop() + yAxisTopMargin;
        int x2 = getWidth()-getPaddingRight();
        int y2 = getHeight()-getPaddingBottom() - yAxisBottomMargin;
        int boxWidth = x2-x1;
        int boxHeight = y2-y1;
        
        int numBars = data.xaxisData.size();
        
        Value calculatedMax = thresholdValue == null ? maxValue : Value.max(thresholdValue, maxValue);
        
        if (numBars > 0) {
            Matrix tx = new Matrix();
            tx.postScale(boxWidth/numBars, -boxHeight);
            tx.postTranslate(x1, y2);
    
            
    
            // Draw the Bars
            // Institute a cliprect of the paintable part of the graph, to contain the dropshadow
            canvas.save();
            RectF bounds = new RectF(0, 0, numBars, 1);
            tx.mapRect(bounds);
            bounds.top -= 1;
            canvas.clipRect(bounds);
            
//            Log.d(NodeUsage.TAG, "Drawing  " + numBars + " bars");
            
            Iterator<Value[]> valIt = data.data.values().iterator();
            for (int i = 0; i < numBars; i++) {
                Value[] values = valIt.next();
                
                Value sum = sumValues(values);

                double totalPerc = sum.divideByValue(calculatedMax);
                RectF totalBar = new RectF(i, 0, i+0.75f, (float) totalPerc);
                tx.mapRect(totalBar);
                canvas.drawRect(totalBar.left+2, totalBar.top+2, totalBar.right+2, totalBar.bottom+2, graphColors.dropShadowPaint);

                
                float startVal = 0.0f;
                int colorIndex = 0;
                for (Value val: values) {
                    if (val != null) {
                        float perc = (float) val.divideByValue(calculatedMax);
                        float endVal = startVal + perc;
                        RectF bar = new RectF(i, startVal, i+0.75f, endVal);
                        tx.mapRect(bar);
                        
                        canvas.drawRect(bar, graphColors.barPaint[colorIndex % graphColors.barPaint.length]);
                        canvas.drawRect(bar, graphColors.barBorderPaint);
                        startVal = endVal;
                    }
                    colorIndex++;
                }
                
            }
            canvas.restore();
    
//            Log.d(NodeUsage.TAG, "Drawing Axis Lines");

            
            // Axis lines
            float[] axisLine = {0,1, 0,0, numBars,0};
            tx.mapPoints(axisLine);
            canvas.drawLine(axisLine[0], axisLine[1], axisLine[2], axisLine[3], mTextPaint);
            canvas.drawLine(axisLine[2], axisLine[3], axisLine[4], axisLine[5], mTextPaint);

//            Log.d(NodeUsage.TAG, "Drawing Ticks");

            // Horizonal Ticks
            Iterator<? extends Object> it = data.xaxisData.iterator();
            for (int i = 0; i < numBars; i++) {
                float[] tickPos = {i+0.375f, 0};
                tx.mapPoints(tickPos);
                Object d = it.next();
                
                canvas.drawLine(tickPos[0], tickPos[1], tickPos[0], tickPos[1] + 5, mTextPaint);
                
                canvas.save();
                canvas.translate((float) (tickPos[0] - 0*mAscent * Math.cos(Math.PI/4)), tickPos[1] - mAscent);
                canvas.rotate(45);
                canvas.drawText(data.formatter.format(d), 0, 0, mTextPaint);
                canvas.restore();
            }
    
            // Vertical Ticks
            int numTicks = 5;
            Value tickPeriod = calculatedMax.divideByNumber(numTicks);
            
            for (int i = 1; i <= numTicks; i++) {
                float[] tickPos = {0, i/(float)numTicks};
                tx.mapPoints(tickPos);
                
                canvas.drawLine(tickPos[0]-5, tickPos[1], tickPos[0], tickPos[1], mTextPaint);
                String label = tickPeriod.multiplyBy(i).toString();
                canvas.drawText(label, tickPos[0] - mTextPaint.measureText(label) - 7, tickPos[1] - (mAscent + mTextPaint.descent())/2.0f, mTextPaint);
            }

            // Draw quota line
            if (thresholdValue != null) {
//                Log.d(NodeUsage.TAG, "Drawing QuotaLine: ");
                float qy = (float) thresholdValue.divideByValue(calculatedMax);
                float[] tickPos = {0, qy, numBars,  qy};
                tx.mapPoints(tickPos);
                canvas.drawLine(tickPos[0], tickPos[1], tickPos[2], tickPos[3], graphColors.mQuotaLinePaint);
            }

        }   
//        Log.d(NodeUsage.TAG, "Finished Drawing");
    }
    

}
