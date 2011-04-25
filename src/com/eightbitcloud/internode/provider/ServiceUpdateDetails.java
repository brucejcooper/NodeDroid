package com.eightbitcloud.internode.provider;

import com.eightbitcloud.internode.data.Plan;
import com.eightbitcloud.internode.data.ServiceIdentifier;
import com.eightbitcloud.internode.data.ServiceType;
import com.eightbitcloud.internode.data.ThingWithProperties;

public class ServiceUpdateDetails extends ThingWithProperties {
    ServiceIdentifier accountNumber;
    ServiceType serviceType = ServiceType.MONTHLY_QUOTA_WITH_EXCESS;
    Plan plan;
    
    public Plan getPlan() {
        return plan;
    }
    
    public void setServiceType(ServiceType typ) {
        this.serviceType = typ;
    }
    
    public ServiceType getServiceType() {
        return serviceType;
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