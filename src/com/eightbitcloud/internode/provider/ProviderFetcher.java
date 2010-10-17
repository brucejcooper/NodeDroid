package com.eightbitcloud.internode.provider;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;

public interface ProviderFetcher {

    void fetchServices(Account account) throws AccountUpdateException;
    void testUsernameAndPassword(Account account) throws AccountUpdateException, WrongPasswordException;
    void updateService(Service service) throws AccountUpdateException;
}
