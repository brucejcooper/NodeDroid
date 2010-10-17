package com.eightbitcloud.internode;

import java.io.IOException;

@SuppressWarnings("serial")
public class ServerErrorException extends IOException {

    public ServerErrorException(String msg) {
        super(msg);
    }
}
