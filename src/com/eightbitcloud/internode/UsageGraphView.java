package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.eightbitcloud.internode.data.MeasuredValue;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.UsageRecord;
import com.eightbitcloud.internode.data.Value;

public class UsageGraphView extends LinearLayout {
    /**
     * When creating the radio buttons, we need to give them an ID.  WE give them this base so that they don't conflict with anything else.
     */
    private static final int GRAPHTYPE_ID_BASE = 0x7f180000;
    private MetricGroup group;
    private UsageGraphType selectedUsageGraphType;
    GraphView graph;

    RadioGroup buttonLayout;

    /**
     * Constructor. This version is only needed if you will be instantiating the
     * object manually (not from a layout XML file).
     * 
     * @param context
     */
    public UsageGraphView(Context context) {
        super(context);
    }

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context,
     *      android.util.AttributeSet)
     */
    public UsageGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        graph = new GraphView(context);
        setOrientation(LinearLayout.VERTICAL);
        buttonLayout =new RadioGroup(context);
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setOrientation(RadioGroup.HORIZONTAL);
        buttonLayout.setPadding(0, 0, 0, 0);
        
        buttonLayout.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    setSelectedGraph(UsageGraphType.values()[checkedId - GRAPHTYPE_ID_BASE]);
                }
            }
        });
        
        addView(buttonLayout,  new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0.0f));
        addView(graph, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
    }

    public void setGraphColors(GraphColors colors) {
        graph.setGraphColors(colors);
    }
    
    public void setMetricGroup(MetricGroup group) {
        this.group = group;
        
        buttonLayout.clearCheck();
        buttonLayout.removeAllViews();
        if (group.getGraphTypes().size() > 1) {
            for (UsageGraphType type: group.getGraphTypes()) {
                RadioButton b = new RadioButton(getContext());
                b.setText(type.getDisplayString());
                b.setBackgroundResource(R.drawable.btn_radio);
                b.setButtonDrawable(R.drawable.btn_radio); // Naughty, but it does stop the button from drawing.
                b.setPadding(5,0,5,0);
                b.setTextSize(12);
//                Log.d(NodeUsage.TAG, "Created button for " + type + " with id of " + type.ordinal());
                b.setId(GRAPHTYPE_ID_BASE + type.ordinal());
                buttonLayout.addView(b);
            }
        }
        
        UsageGraphType newGraphType = selectedUsageGraphType;
        
        if (!group.getGraphTypes().contains(newGraphType)) {
            newGraphType = group.getGraphTypes().get(0);
        }
        this.selectedUsageGraphType = null;
        setSelectedGraph(newGraphType);
        
        requestLayout();
        invalidate();
    }

    public void setSelectedGraph(UsageGraphType usageGraphType) {
        if (this.selectedUsageGraphType != usageGraphType) {
            this.selectedUsageGraphType = usageGraphType;
//            Log.d(NodeUsage.TAG, "Selected graphType is " + usageGraphType + ", Selecting " + usageGraphType.ordinal() + " Current selection is " + buttonLayout.getCheckedRadioButtonId());
            buttonLayout.check(GRAPHTYPE_ID_BASE + usageGraphType.ordinal());
            
            
            // GenerateData.
            switch (usageGraphType) {
            case BREAKDOWN:
                generateBreakdown();
                break;
            case MONTHLY_USAGE:
                generateMonthly();
                break;
            case YEARLY_USAGE:
                generateYearly();
                break;
            }
            
            
            refreshDrawableState();
        }
    }

    private void generateYearly() {
        SortedMap<Date, Value[]> yearStats = new TreeMap<Date, Value[]>();
        Date monthEnd = group.getService().getPlan().getNextRollover();
        if (monthEnd != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(monthEnd);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int count = group.getComponents().size();

            List<MeasuredValue> values = getOrderedValues();

            
            for (int i = 0; i < 12; i++) {
                cal.setTime(monthEnd);
                cal.add(Calendar.MONTH, -1);
                Date monthBegin = cal.getTime();

                int index = 0;
                for (MeasuredValue mv: values) {
                    SortedMap<Date, UsageRecord> usage = mv.getUsageRecords();
                    Value sum = new Value(0, mv.getUnits());
                    SortedMap<Date, UsageRecord> monthEntries = usage.subMap(monthBegin, monthEnd);
                    for (UsageRecord dayValue : monthEntries.values()) {
                        sum = sum.plus(dayValue.getAmount());
                    }

                    Value[] entries = yearStats.get(monthBegin);
                    if (entries == null) {
                        entries = new Value[count];
                        yearStats.put(monthBegin, entries);
                    }
                    entries[index] = sum;
                    index++;
                }
                monthEnd = monthBegin;
            }
            graph.setData(new GraphData<Date,Value>(yearStats, GraphStyle.BAR, DateKeyFormatter.YEAR_FORMATTER));
            graph.thresholdValue = group.getAllocation();

        }        
        
    }
    
    private List<MeasuredValue> getOrderedValues() {
        List<MeasuredValue> values = new ArrayList<MeasuredValue>(group.getComponents());
        Collections.sort(values, Collections.reverseOrder());
        return values;
        
    }

    private void generateMonthly() {
        SortedMap<Date, Value[]> monthStats = new TreeMap<Date, Value[]>();
        Plan plan = group.getService().getPlan();
        if (plan != null) {
            Date periodStart = plan.getPreviousRollover();
            Date rollover = plan.getNextRollover();
            if (periodStart != null) {
                Calendar cal = Calendar.getInstance(); // TODO make it respect the TZ
                cal.setTime(periodStart);
                int count = group.getComponents().size();
                
                List<MeasuredValue> values = getOrderedValues();
                while (periodStart.before(rollover)) {
                    cal.setTime(periodStart);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    Date periodEnd = cal.getTime();
    
                    int index = 0;
                    for (MeasuredValue mv: values) {
                        SortedMap<Date, UsageRecord> usage = mv.getUsageRecords();
                        Value sum = new Value(0, mv.getUnits());
                        SortedMap<Date, UsageRecord> monthEntries = usage.subMap(periodStart, periodEnd);
                        for (UsageRecord dayValue : monthEntries.values()) {
                            sum = sum.plus(dayValue.getAmount());
                        }
    
                        Value[] entries = monthStats.get(periodStart);
                        if (entries == null) {
                            entries = new Value[count];
                            monthStats.put(periodStart, entries);
                        }
                        entries[index] = sum;
                        index++;
                    }
                    periodStart = periodEnd;
                }
                graph.setData(new GraphData<Date,Value>(monthStats, GraphStyle.BAR, DateKeyFormatter.MONTH_FORMATTER));
                graph.thresholdValue = null;
    
            }        
        }
        
    }

    
    
//
//    private void generateMonthly() {
//        
//        
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        cal.add(Calendar.MONTH, -1);
//        cal.set(Calendar.HOUR_OF_DAY, 23);
//        cal.set(Calendar.MINUTE, 59);
//        cal.set(Calendar.SECOND, 59);
//        cal.set(Calendar.MILLISECOND, 999);
//        
//        SortedMap<Date, Value[]> graphData = new TreeMap<Date,Value[]>();
//        int index = 0;
//        int count = group.getComponents().size();
//        for (MeasuredValue mv: group.getComponents()) {
//            SortedMap<Date, UsageRecord> usage = mv.getUsageRecords();
//            SortedMap<Date, UsageRecord> lastMonth = usage.tailMap(cal.getTime());
//            for (Map.Entry<Date,UsageRecord> e: lastMonth.entrySet()) {
//                Value[] entries = graphData.get(e.getKey());
//                if (entries == null) {
//                    entries = new Value[count];
//                    graphData.put(e.getKey(), entries);
//                }
//                entries[index] = e.getValue().getAmount();
//            }
//            index++;
//        }
//
//        graph.setData(new GraphData<Date,Value>(graphData, GraphStyle.BAR, DateKeyFormatter.MONTH_FORMATTER));
//        graph.thresholdValue = null;
//    }

    private void generateBreakdown() {
        List<MeasuredValue> values = getOrderedValues();
        Collections.sort(values, Collections.reverseOrder());
        Map<MeasuredValue, Value[]> data = new HashMap<MeasuredValue, Value[]>();
        for (MeasuredValue v: values) {
            data.put(v, new Value[] {v.getAmount()});
        }
        graph.setData(new GraphData<MeasuredValue,Value>(values, data, GraphStyle.PIE, new KeyFormatter() {
            public String format(Object key) {
                MeasuredValue v = ((MeasuredValue) key);
                return v.getAmount() + " of " + v.getName();
            }
        }));
        graph.thresholdValue = null;
        
        
    }
    
    

}
