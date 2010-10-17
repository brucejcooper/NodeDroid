package com.eightbitcloud.internode.data;

import com.eightbitcloud.internode.R;

public enum CounterStyle {
    QUOTA(R.layout.mg_cap_summary),
    DRAWDOWN(R.layout.mg_cap_summary),
    CAP(R.layout.mg_cap_summary),
    SIMPLE(R.layout.mg_simple_summary);
    
    int id;
    
    CounterStyle(int id) {
        this.id = id;
    }
    
    public int getLayoutId() {
        return id;
    }
}
