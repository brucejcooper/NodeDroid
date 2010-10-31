package com.eightbitcloud.internode.data;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class UsageRecord implements PreferencesSerialisable {
    private static final String DESCRIPTION2 = "description";
    private static final String COST2 = "cost";
    private static final String AMOUNT2 = "amount";
    private static final String TIME2 = "time";
    private Date time;
    private Value amount;
    private Value cost;
    private String description;
    
    public UsageRecord(Date day, Value amount) {
        this.time = day;
        this.amount = amount;
        this.cost = new Value(0, Unit.CENT);
        this.description = "";
    }
    
    public UsageRecord() {
        
    }
    
    public Date getTime() {
        return time;
    }
    public void setTime(Date time) {
        this.time = time;
    }
    public Value getAmount() {
        return amount;
    }
    public void setAmount(Value amount) {
        this.amount = amount;
    }
    public Value getCost() {
        return cost;
    }
    public void setCost(Value cost) {
        assert cost.getUnit() == Unit.CENT;
        this.cost = cost;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public void writeTo(JSONObject obj) throws JSONException {
        obj.put(TIME2, time.getTime());
        obj.put(AMOUNT2, amount.getPrefValue());
        obj.put(COST2, cost.getPrefValue());
        obj.put(DESCRIPTION2, description);
    }

    public void readFrom(JSONObject obj) throws JSONException {
        time = new Date(obj.getLong(TIME2));
        amount = new Value(obj.getString(AMOUNT2));
        cost = new Value(obj.getString(COST2));
        description = obj.getString(DESCRIPTION2);
        
    }

}
