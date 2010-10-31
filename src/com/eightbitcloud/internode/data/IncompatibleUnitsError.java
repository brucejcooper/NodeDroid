package com.eightbitcloud.internode.data;

@SuppressWarnings("serial")
public class IncompatibleUnitsError extends RuntimeException {

    public IncompatibleUnitsError() {
        super("Incompatible units");
    }
}
