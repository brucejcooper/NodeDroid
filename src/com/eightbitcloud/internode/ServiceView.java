package com.eightbitcloud.internode;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eightbitcloud.internode.data.CounterStyle;
import com.eightbitcloud.internode.data.MetricGroup;
import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.data.Value;
import com.eightbitcloud.internode.util.DateTools;

public class ServiceView extends FrameLayout {

    private Service service;
    private ListView metricGroupView;
    private MetricGroupListAdapter listAdapter;
    LayoutInflater inflater;
    private MetricGroup selectedMetricGroup;
    
    UsageGraphView graphView;
    private boolean loading;

    public ServiceView(Context context, AttributeSet attrs, final Typeface font) {
        super(context, attrs);
        
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.quota, this);

        ((TextView) findViewById(R.id.serviceid)).setTypeface(font);
        ((TextView) findViewById(R.id.planlabel)).setTypeface(font);
        ((TextView) findViewById(R.id.usagelabel)).setTypeface(font);
        ((TextView) findViewById(R.id.rolloverlabel)).setTypeface(font);
        // ((TextView) findViewById(R.id.ipaddresslabel)).setTypeface(font);
        ((TextView) findViewById(R.id.avgusagelabel)).setTypeface(font);
        ((TextView) findViewById(R.id.bottomheader)).setTypeface(font);

        
        
        graphView = (UsageGraphView) findViewById(R.id.graph);

        
        metricGroupView = (ListView) findViewById(R.id.metricgrouplist);
        listAdapter = new MetricGroupListAdapter(context, service, font);
        metricGroupView.setAdapter(listAdapter);
        metricGroupView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        metricGroupView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MetricGroup group = (MetricGroup) listAdapter.getItem(position);
                setSelectedMetricGroup(group);
            }
        });
        
    }

    public void setService(Service service) {
        this.service = service;
        listAdapter.setService(service);
        
        Provider prov = service.getAccount().getProvider();
        int resid = getResources().getIdentifier(prov.getBackgroundResource(), "drawable", this.getClass().getPackage().getName());
        TextView serviceID = (TextView) findViewById(R.id.serviceid);
        
        serviceID.setBackgroundResource(resid);
        serviceID.setTextColor(Color.parseColor(prov.getTextColour()));
        graphView.setGraphColors(service.getAccount().getProvider().getGraphColors());
        
        
        update();
    }
    
    public Service getService() {
        return this.service;
    }

    
    public synchronized void setLoading(boolean loading) {
        
        boolean runningBefore = this.loading;
        this.loading = loading;
        boolean runningAfterwards = loading;


        if (runningBefore ^ runningAfterwards) {
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
            if (runningBefore) {
                // We're turing it off
                progressBar.setVisibility(View.INVISIBLE);
            } else {
                // We're turning it on.
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    

    public void updateGraph(MetricGroup mg) {
        if (mg != null) {
//            Plan plan = service.getPlan();
//            Value quota = mg.getAllocation();
            graphView.setMetricGroup(mg);
        }     
    }

    public void update() {
        Plan plan = service.getPlan();

        ((TextView) findViewById(R.id.serviceid)).setText(service.getIdentifier().toString());
        ((TextView) findViewById(R.id.plan)).setText(plan == null ? "" : plan.getName());

        
        MetricGroup group = getSelectedMetricGroup();
        if (group != null) {
    
            Value remainingQuota = group.getAllocation().minus(group.getValue());
    
            TextView usageTextView =((TextView) findViewById(R.id.usage));
            
            if (plan != null && plan.getNextRollover() != null) {
                
                long now = System.currentTimeMillis();
                long timeElapsed = now - plan.getPreviousRollover().getTime();
               
                long timeRemaining = plan.getNextRollover().getTime() - now;
                long daysLeft = (long)Math.ceil(timeRemaining / (double)DateTools.DAY);

                Value avgUsage;
                if (timeElapsed < DateTools.DAY) {
                    avgUsage = group.getValue();
                } else {
                    avgUsage = group.getValue().divideByNumber(timeElapsed / DateTools.DAY);
                }


                
                
                String txt = plan.getNextRollover() == null ? "" : DateTools.LOCAL_DATE_FORMAT.format(plan.getNextRollover());
                if (timeRemaining < 0) {
                    txt = txt + " - expired! New month";
                
                } else if (timeRemaining <= DateTools.DAY) {
                    txt = txt + " Ð <1 day left";
                } else {
                    txt = txt + " Ð " + daysLeft + " days left";
                }
                ((TextView) findViewById(R.id.rollover)).setText(txt);

                
                
                
                switch (group.getStyle()) {
                case SIMPLE:
                    usageTextView.setText(group.getValue().toString());
                    ((TextView) findViewById(R.id.avgusage)).setText(avgUsage.toString() + "/day");
                    break;
                default:
                    if (remainingQuota.getAmt() == 0) {
                        usageTextView.setText(String.format("%s of %s", group.getValue(), group.getAllocation()));
                    } else if (remainingQuota.getAmt() <= 0) {
                        usageTextView.setText(String.format("%s of %s - over!", group.getValue(), group.getAllocation()));
                    } else {
                        usageTextView.setText(String.format("%s of %s - %s left", group.getValue(), group.getAllocation(), remainingQuota));
                    }
        
                    String remainder;
                    if (daysLeft == 0) {
                        remainder = remainingQuota.toString();
                    } else {
                        remainder = remainingQuota.divideByNumber(daysLeft).toString() + "/day";
                    }
        
                    ((TextView) findViewById(R.id.avgusage)).setText(avgUsage.toString() + "/day, remainder: " +remainder);
                    break;
                        
                }
            
            }

    
            updateGraph(group);
            
            listAdapter.notifyDataSetChanged();
        }

        
    }
    
    public void setSelectedMetricGroup(MetricGroup g) {
        this.selectedMetricGroup = g;
        ((TextView)findViewById(R.id.bottomheader)).setText(g == null ? "" : g.getName());
        update();
    }
    
    public MetricGroup getSelectedMetricGroup() {
        if (selectedMetricGroup == null) {
            List<MetricGroup> all = service.getAllMetricGroups();
            if (!all.isEmpty()) {
                setSelectedMetricGroup(all.get(0));
            }
        }
        return selectedMetricGroup;
    }

    static class ViewHolder {
        CounterStyle style;
        TextView value;
        TextView name;
        QuotaBarGraph graph; 
    }


}
