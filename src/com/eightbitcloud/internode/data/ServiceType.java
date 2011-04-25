package com.eightbitcloud.internode.data;

public enum ServiceType {
    /**
     * Represents a montly quota service, which charges extra if you go over the quota.  This is a traditional Phone CAP plan
     */
    MONTHLY_QUOTA_WITH_EXCESS, 
    
    /**
     * Represents a monthly quota service, which imposes limits (degraded service or restricted servce) when the quota is exhausted.  A Broadband with download restrictions would be an example of this one
     */
    MONTHY_QUOTA_WITH_LIMITS,
    
    /**
     * A Service that you top up with funds, and then exhaust at your leisure.  The service may still have an expiry, but it might not too.
     */
    PREPAID
}
