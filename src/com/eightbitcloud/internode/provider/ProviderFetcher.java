package com.eightbitcloud.internode.provider;

import java.util.List;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;

public interface ProviderFetcher {
    List<ServiceUpdateDetails> fetchAccountUpdates(Account account) throws AccountUpdateException, InterruptedException, WrongPasswordException;
    Service fetchServiceDetails(Account account, ServiceUpdateDetails identifier) throws AccountUpdateException, InterruptedException, WrongPasswordException;
    void testUsernameAndPassword(Account account) throws AccountUpdateException, WrongPasswordException, InterruptedException;
    
    void setLogTraffic(boolean val);
    
    void cleanup();
}
