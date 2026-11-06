package com.bankingPlatform.core.account.mapper;

import com.bankingPlatform.core.account.dto.AccountRequest;
import com.bankingPlatform.core.account.entity.Account;

import java.math.BigDecimal;

public class AccountMapper {

    public static Account mapAccount(AccountRequest dto) {
        if (dto == null) return null;

        Account account = new Account();
        account.setUserId(dto.getUserId());
        account.setBalance(BigDecimal.ZERO);
        account.setCpf(dto.getCpf());
        return account;
    }

//    public static AccountDTO mapAccountDto(Account account) {
//        if (account == null) return null;
//
//        AccountDTO dto = new AccountDTO();
//        dto.setAccountHolderName(account.getAccountHolderName());
//        dto.setCpf(account.getCpf());
//        dto.setUsername();
//        return dto;
//
//
//   }
}
