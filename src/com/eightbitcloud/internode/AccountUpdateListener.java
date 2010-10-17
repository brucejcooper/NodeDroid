package com.eightbitcloud.internode;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.Service;

public interface AccountUpdateListener {
    public void accountLoadStarted(Account account);
    public void accountLoadCompletedSuccessfully(Account account);
    public void errorUpdatingAccounts(Account account, Exception ex);
    
    
    public void serviceLoadStarted(Service service);
    public void serviceUpdatedCompletedSuccessfully(Service service);
    public void errorUpdatingService(Service service, Exception ex);

}
