package com.eightbitcloud.internode;

public enum UsageGraphType {
    BREAKDOWN("Breakdown"),
    MONTHLY_USAGE("Month"),
    YEARLY_USAGE("Year"),
    ALL_USAGE("All");
    
    private String displayName;

    UsageGraphType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayString() {
        return displayName;
    }
}
