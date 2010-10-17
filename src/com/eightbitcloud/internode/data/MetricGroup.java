package com.eightbitcloud.internode.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.eightbitcloud.internode.UsageGraphType;

public class MetricGroup implements NamedThing {
    private Service service;
    private String name;
    private CounterStyle style = CounterStyle.QUOTA;
    private Value allocation ;
    private NameMappedList<MeasuredValue> components = new NameMappedList<MeasuredValue>();
    private boolean excessCharged;
    private boolean excessShaped;
    private boolean excessRestrictAccess;
    private Unit units;
    private List<UsageGraphType> graphTypes = Collections.singletonList(UsageGraphType.MONTHLY_USAGE);
    
    public MetricGroup(Service service, String name, Unit units, CounterStyle style) {
        this.service = service;
        this.units = units;
        this.name = name;
        this.allocation = new Value(0, units);
        this.style = style;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public CounterStyle getStyle() {
        return style;
    }
    public void setStyle(CounterStyle style) {
        this.style = style;
    }
    public Value getAllocation() {
        return allocation;
    }
    public void setAllocation(Value allocation) {
        this.allocation = allocation;
    }
    public List<MeasuredValue> getComponents() {
        return components;
    }
    public void setComponents(Collection<MeasuredValue> components) {
        for (MeasuredValue c: components) {
            addComponent(c);
        }
    }
    public void addComponent(MeasuredValue c) {
        components.add(c);
        
    }
    public MeasuredValue getComponent(String usageValue) {
        return components.getItemNamed(usageValue);
    }
    public void setExcessCharged(boolean parseBoolean) {
        excessCharged = parseBoolean;
        
    }
    public void setExcessShaped(boolean parseBoolean) {
        excessShaped = parseBoolean;
        
    }
    public void setExcessRestrictAccess(boolean parseBoolean) {
        excessRestrictAccess = parseBoolean;
        
    }
    public boolean isExcessCharged() {
        return excessCharged;
    }
    public boolean isExcessShaped() {
        return excessShaped;
    }
    public boolean isExcessRestrictAccess() {
        return excessRestrictAccess;
    }
    public Value getValue() {
        Value sum = new Value(0, units);
        for (MeasuredValue val: components) {
            sum = sum.plus(val.getAmount());
        }
        return sum;
    }

    public List<UsageGraphType> getGraphTypes() {
        return graphTypes;
    }

    public void setGraphTypes(UsageGraphType... graphTypes) {
        setGraphTypes(Arrays.asList(graphTypes));
    }
    
    public void setGraphTypes(List<UsageGraphType> graphTypes) {
        this.graphTypes = graphTypes;
    }

    public Service getService() {
        return service;
    }
    
    @Override
    public String toString() {
        return name;
    }
    

}
