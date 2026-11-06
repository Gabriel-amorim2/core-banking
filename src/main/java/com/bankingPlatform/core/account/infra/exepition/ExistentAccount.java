package com.bankingPlatform.core.account.infra.exepition;

public class ExistentAccount extends RuntimeException {

    public ExistentAccount(String message) {
        super(message);
    }
}
