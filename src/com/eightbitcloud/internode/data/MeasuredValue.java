package com.eightbitcloud.internode.data;

import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

public class MeasuredValue implements NamedThing, Comparable<MeasuredValue> {
    private String name;
    private Value amount;
    private SortedMap<Date, UsageRecord> usageRecords = new TreeMap<Date, UsageRecord>();
    private Unit units;
    
    
    public MeasuredValue(Unit units) {
        this.units = units;
        this.amount = new Value(0, units);
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

}
