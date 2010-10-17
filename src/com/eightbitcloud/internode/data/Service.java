package com.eightbitcloud.internode.data;

import java.util.List;

public class Service extends ThingWithProperties {
    
    private Account account;
    private String identifier;
    private String name;
    private Plan plan;
    private NameMappedList<MetricGroup> metricGroups = new NameMappedList<MetricGroup>();

    
    public void setMetricGroups(List<MetricGroup> metricGroups) {
        this.metricGroups.clear();
        for (MetricGroup g: metricGroups) {
            addMetricGroup(g);
        }
    }
    public void addMetricGroup(MetricGroup g) {
        metricGroups.add(g);
    }
    public Account getAccount() {
        return account;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Plan getPlan() {
        return plan;
    }
    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public MetricGroup getMetricGroup(String metricGroupName) {
        return metricGroups.getItemNamed(metricGroupName);
    }
    
    public List<MetricGroup> getAllMetricGroups() {
        return metricGroups;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[Service ").append(getIdentifier()).append('/').append(name).append(" on plan ").append(plan).append(". Groups: ").append(metricGroups).append("]");
        
        return sb.toString();
    }
    public int getMetricGroupCount() {
        
       return metricGroups.size();
    }
    

}
