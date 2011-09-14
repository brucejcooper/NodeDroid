package com.eightbitcloud.internode.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class Account  {
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.eightbitcloud.internode.account";
    public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.eightbitcloud.internode.account";

    public static final String ID = "_id";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String PROVIDER = "provider";
    
    
    private int id;
    private String username;
    private String password;
    private String provider;
    

    public Account() {
    }
    
    @Override
    public String toString() {
        return provider + "/" + username;
    }
    
    public Account(Cursor cursor) {
        int idCol = cursor.getColumnIndex(ID);
        int usernameCol = cursor.getColumnIndex(USERNAME);
        int passwordCol = cursor.getColumnIndex(PASSWORD);
        int providerCol = cursor.getColumnIndex(PROVIDER);
        
        this.id = cursor.getInt(idCol);
        this.username = cursor.getString(usernameCol);
        this.password = cursor.getString(passwordCol);
        this.provider = cursor.getString(providerCol);
    }
    
    public Uri getUri() {
        return ContentUris.withAppendedId(AccountProvider.ACCOUNTS_CONTENT_URI, id); 
    }
    
    public ContentValues getValues() {
        ContentValues vals = new ContentValues();
        if (id != 0) {
            vals.put(ID, id);
        }
        vals.put(USERNAME, username);
        vals.put(PASSWORD, password);
        vals.put(PROVIDER, provider);
        return vals;
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
        return ProviderStore.getInstance().getProvider(provider.toLowerCase());
    }

    public void setProviderName(String provider) {
        this.provider = provider;
    }
    
    public String getProviderName() {
        return provider;
    }

    public int getId() {
        return id;
    }
}
