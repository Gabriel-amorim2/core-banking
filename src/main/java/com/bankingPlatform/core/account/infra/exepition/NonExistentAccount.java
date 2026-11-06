package com.bankingPlatform.core.account.infra.exepition;

public class NonExistentAccount extends RuntimeException {

    public NonExistentAccount(String message){
        super(message);
    }
}
