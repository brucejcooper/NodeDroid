package com.eightbitcloud.internode.data;

import java.util.Date;

public class UsageRecord {
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

}
