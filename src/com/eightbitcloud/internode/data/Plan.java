package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eightbitcloud.internode.util.DateTools;


public class Plan {
    private String description;
    private String name;
    private Date rolloverDate;
    private Map<String, Object> properties = new HashMap<String, Object>();
    private Value cost;
    private PlanInterval interval;
    private List<String> extras = new ArrayList<String>();
    private Date cachedPreviousRollover;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNextRollover(Date date) {
        this.rolloverDate = date;
        this.cachedPreviousRollover = null;
    }

    public Date getNextRollover() {
        return rolloverDate;
    }
    
    public Date getPreviousRollover() {
        if (cachedPreviousRollover == null) {
            Date next = getNextRollover();
            if (next != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(next);
                cal.add(Calendar.MONTH, -1);
                cachedPreviousRollover =  cal.getTime();
            }
        }
        return cachedPreviousRollover;
    }
    
    public long getBillingPeriodLength() {
        return getNextRollover().getTime() - getPreviousRollover().getTime();
    }
    
    public double getPercentgeThroughMonth(long now) {
        long timeElapsed = now - getPreviousRollover().getTime();
        return (timeElapsed / (double) getBillingPeriodLength());
    }
    

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setCost(Value cost) {
        assert cost.getUnit() == Unit.CENT;
        this.cost = cost;
    }

    public Value getCost() {
        return cost;
    }

    public void setInterval(PlanInterval valueOf) {
        interval = valueOf;
    }
    
    public PlanInterval getInterval() {
        return interval;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(cost).append(' ').append(name).append(" which rolls over on " + rolloverDate);
        return sb.toString();
    }

    public void addPlanExtra(String extra) {
        extras.add(extra);
    }
    
    public List<String> getPlanExtras() {
        return extras ;
    }

}
