package com.eightbitcloud.internode.provider;

import java.util.List;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;

public interface ProviderFetcher {
    List<ServiceUpdateDetails> fetchAccountUpdates(Account account) throws AccountUpdateException, InterruptedException;
    void fetchServiceDetails(Service service) throws AccountUpdateException, InterruptedException;
    void testUsernameAndPassword(Account account) throws AccountUpdateException, WrongPasswordException;
    
    void setLogTraffic(boolean val);
}
