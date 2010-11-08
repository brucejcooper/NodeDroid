package com.eightbitcloud.internode.provider;

import java.io.IOException;

@SuppressWarnings("serial")
public class WrongPasswordException extends IOException {

    public WrongPasswordException() {
        super("username or password incorrect");
    }
}
