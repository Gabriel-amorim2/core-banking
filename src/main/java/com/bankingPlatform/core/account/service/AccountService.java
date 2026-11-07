package com.bankingPlatform.core.account.service;

import com.bankingPlatform.core.account.dto.AccountRequest;
import com.bankingPlatform.core.account.dto.AccountResponse;

import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(AccountRequest accountRequest);
    AccountResponse getMyAccounts(UUID userId);
    AccountResponse deleteAccounts(UUID userId);

}
