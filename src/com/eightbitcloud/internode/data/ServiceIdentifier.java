package com.eightbitcloud.internode.data;

import java.io.Serializable;

import android.content.ContentUris;
import android.net.Uri;

public class ServiceIdentifier implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 7852995063261519877L;
    private String provider;
    private String accountNumber;

    public ServiceIdentifier(String accountSource, String accountNumber) {
        this.provider = accountSource;
        this.accountNumber = accountNumber;
    }
    

    public String getProvider() {
        return provider;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accountNumber == null) ? 0 : accountNumber.hashCode());
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceIdentifier other = (ServiceIdentifier) obj;
        if (accountNumber == null) {
            if (other.accountNumber != null)
                return false;
        } else if (!accountNumber.equals(other.accountNumber))
            return false;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return provider + "/" + accountNumber;
    }
}
