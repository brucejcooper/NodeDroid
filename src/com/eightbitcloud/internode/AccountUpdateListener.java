package com.eightbitcloud.internode;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;

public interface AccountUpdateListener {
    public void startingServiceFetchForAccount(Account account);
    public void fetchedServiceNamesForAccount(Account account);
    public void errorUpdatingServices(Account account, Exception ex);
    
    
    public void serviceUpdateStarted(Service service);
    public void serviceUpdated(Service service);
    public void errorUpdatingService(Service service, Exception ex);

}
