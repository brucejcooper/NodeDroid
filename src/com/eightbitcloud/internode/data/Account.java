package com.eightbitcloud.internode.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Account extends ThingWithProperties {
    private String username;
    private String password;
    private Provider provider;
    private Map<String, Service> services = new HashMap<String, Service>();

    public Service getService(String accountID) {
        return services.get(accountID);
    }

    public void addService(Service service) {
        service.setAccount(this);
        this.services.put(service.getIdentifier(), service);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Collection<Service> getAllServices() {
        return services.values();
    }
}
