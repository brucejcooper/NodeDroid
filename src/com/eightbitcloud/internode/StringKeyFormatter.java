package com.eightbitcloud.internode;

public class StringKeyFormatter implements KeyFormatter {

    @Override
    public String format(Object key) {
        return key == null ? "NULL" : key.toString();
    }

}
