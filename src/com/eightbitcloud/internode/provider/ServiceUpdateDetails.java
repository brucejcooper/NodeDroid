package com.eightbitcloud.internode.provider;

import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.data.ThingWithProperties;

public class ServiceUpdateDetails extends ThingWithProperties {
    ServiceIdentifier accountNumber;
    Plan plan;
    
    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public ServiceUpdateDetails(ServiceIdentifier accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public ServiceIdentifier getIdentifier() {
        return accountNumber;
    }
}