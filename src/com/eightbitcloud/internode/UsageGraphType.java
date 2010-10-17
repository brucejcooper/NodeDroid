package com.eightbitcloud.internode;

public enum UsageGraphType {
    BREAKDOWN("Breakdown"),
    MONTHLY_USAGE("Month"),
    YEARLY_USAGE("Year");
    
    private String displayName;

    UsageGraphType(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
