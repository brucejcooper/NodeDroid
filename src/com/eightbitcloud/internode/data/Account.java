package com.eightbitcloud.internode.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Account extends ThingWithProperties {
    private String username;
    private String password;
    private Provider provider;
    private Map<ServiceIdentifier, Service> services = new HashMap<ServiceIdentifier, Service>();

    public synchronized Service getService(ServiceIdentifier accountID) {
        return services.get(accountID);
    }

    public synchronized void addService(Service service) {
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

    public synchronized Collection<Service> getAllServices() {
        return new ArrayList<Service>(services.values());
    }

    public synchronized boolean removeService(Service service) {
        Service removed = services.remove(service.getIdentifier());
        if (removed != null) {
            removed.setAccount(null);
            return true;
        }
        return false;
        
    }
}
