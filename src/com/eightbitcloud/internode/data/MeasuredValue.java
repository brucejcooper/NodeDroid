package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.eightbitcloud.internode.PreferencesSerialiser;

public class MeasuredValue implements NamedThing, Comparable<MeasuredValue>, PreferencesSerialisable {
    private static final String USAGE_RECORDS = "usageRecords";
    private static final String UNITS2 = "units";
    private static final String AMOUNT2 = "amount";
    private static final String NAME2 = "name";
    private String name;
    private Value amount;
    private SortedMap<Date, UsageRecord> usageRecords = new TreeMap<Date, UsageRecord>();
    private Unit units;
    
    
    public MeasuredValue(Unit units) {
        this.units = units;
        this.amount = new Value(0, units);
    }
    
    public MeasuredValue() {
        
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getAmount() {
        return amount;
    }

    public void setAmount(Value amount) {
        this.amount = amount;
    }

    public SortedMap<Date, UsageRecord> getUsageRecords() {
        return usageRecords;
    }

    public void addUsageRecord(UsageRecord usageRecord) {
        usageRecords.put(usageRecord.getTime(), usageRecord);
    }

    public Unit getUnits() {
        return units;
    }

    public int compareTo(MeasuredValue another) {
        if (this.units != another.units) {
            throw new IncompatibleUnitsError();
        }
        long diff = this.getAmount().getAmt() - another.getAmount().getAmt();
        return diff < 0 ? -1 : diff == 0 ? 0 : 1;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public void clearUsageRecords() {
        usageRecords.clear();
    }

    public void writeTo(JSONObject obj) throws JSONException {
        obj.put(NAME2, name);
        obj.put(AMOUNT2, amount.getPrefValue());
        obj.put(UNITS2, units.toString());
        obj.put(USAGE_RECORDS, PreferencesSerialiser.createJSONRepresentation(usageRecords.values()));
    }

    public void readFrom(JSONObject obj) throws JSONException {
        name = obj.getString(NAME2);
        amount = new Value(obj.getString(AMOUNT2));
        units = Unit.valueOf(obj.getString(UNITS2));
        
        List<UsageRecord> usage = new ArrayList<UsageRecord>();
        PreferencesSerialiser.populateJSONList(usage, UsageRecord.class, obj.getJSONArray(USAGE_RECORDS));
        this.usageRecords = new TreeMap<Date, UsageRecord>();
        for (UsageRecord r: usage) {
            this.usageRecords.put(r.getTime(), r);
        }
    }

}
