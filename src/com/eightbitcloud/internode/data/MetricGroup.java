package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.eightbitcloud.internode.PreferencesSerialiser;
import com.eightbitcloud.internode.UsageGraphType;

public class MetricGroup implements NamedThing, PreferencesSerialisable {
    
    private static final String GRAPH_TYPES = "graphTypes";
    private static final String UNITS2 = "units";
    private static final String EXCESS_RESTRICT_ACCESS = "excessRestrictAccess";
    private static final String EXCESS_SHAPED = "excessShaped";
    private static final String EXCESS_CHARGED = "excessCharged";
    private static final String COMPONENTS2 = "components";
    private static final String ALLOCATION2 = "allocation";
    private static final String STYLE2 = "style";
    private static final String NAME2 = "name";
    
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
    
    public MetricGroup() {
        // Used by Preference serialisation
    }
    
    /**
     * This should only be used by the preference serialisation stuff
     * @param service
     */
    public void __setService(Service service) {
        this.service = service;
    }
    
    @Override
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

    @Override
    public void writeTo(JSONObject obj) throws JSONException {
         obj.put(NAME2, name);
         obj.put(STYLE2, style.toString());
         obj.put(ALLOCATION2, allocation.getPrefValue());
         obj.put(COMPONENTS2, PreferencesSerialiser.createJSONRepresentation(components));
         obj.put(EXCESS_CHARGED, excessCharged);
         obj.put(EXCESS_SHAPED, excessShaped);
         obj.put(EXCESS_RESTRICT_ACCESS, excessRestrictAccess);
         obj.put(UNITS2, units.toString());
         
         obj.put(GRAPH_TYPES, PreferencesSerialiser.createJSONRepresentationForStrings(graphTypes));
    }

    @Override
    public void readFrom(JSONObject obj) throws JSONException {
        name = obj.getString(NAME2);
        style = CounterStyle.valueOf(obj.getString(STYLE2));
        allocation = new Value(obj.getString(ALLOCATION2));
        PreferencesSerialiser.populateJSONList(this.components = new NameMappedList<MeasuredValue>(), MeasuredValue.class, obj.getJSONArray(COMPONENTS2));
        excessCharged = obj.getBoolean(EXCESS_CHARGED);
        excessShaped = obj.getBoolean(EXCESS_SHAPED);
        excessRestrictAccess = obj.getBoolean(EXCESS_RESTRICT_ACCESS);
        units = Unit.valueOf(obj.getString(UNITS2));


        List<String> vals = PreferencesSerialiser.createStringArray(obj.getJSONArray(GRAPH_TYPES));
        graphTypes = new ArrayList<UsageGraphType>(vals.size());
        for (String s: vals) {
            graphTypes.add(UsageGraphType.valueOf(s));
        }
        
    }
    

}
