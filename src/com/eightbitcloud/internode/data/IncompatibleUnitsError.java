package com.eightbitcloud.internode.data;

public class IncompatibleUnitsError extends RuntimeException {

    public IncompatibleUnitsError() {
        super("Incompatible units");
    }
}
