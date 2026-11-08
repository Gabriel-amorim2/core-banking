package com.bankingPlatform.core.account.service;

import com.bankingPlatform.core.account.dto.AccountRequest;
import com.bankingPlatform.core.account.dto.AccountResponse;
import com.bankingPlatform.core.account.entity.Account;
import com.bankingPlatform.core.account.infra.exepition.ExistentAccount;
import com.bankingPlatform.core.account.infra.exepition.NonExistentAccount;
import com.bankingPlatform.core.account.mapper.AccountMapper;
import com.bankingPlatform.core.account.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountServiceImp  implements AccountService {


    private final AccountRepository accountRepository;

    public AccountServiceImp(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountResponse createAccount(AccountRequest accountRequest) {
        if (accountRepository.findByUserId(accountRequest.getUserId()).isPresent()){
            throw new ExistentAccount("the account with this userId already exists");
        }
        Account account = AccountMapper.mapAccount(accountRequest);
        accountRepository.save(account);

        return AccountResponse.builder().message("conta criada")
                .userId(account.getUserId())
                .balance(account.getBalance()).build();


    }
    @Override
    @Transactional
    public AccountResponse deleteAccounts(UUID userId) {

        var acccount = accountRepository.findByUserId(userId).orElseThrow(() -> new NonExistentAccount("conta não existe para deletar"));

        accountRepository.deleteByUserId(userId);

        return AccountResponse.builder().message("Conta deletada")
                .userId(acccount.getUserId())
                .balance(acccount.getBalance())
                .build();
    }


    @Override
    public AccountResponse getMyAccounts(UUID userId) {
        Optional<Account> response =  accountRepository.findByUserId(userId);
        if (response.isEmpty()){
            throw new NonExistentAccount("conta inexistente");
        }
        var account = response.get();
        return AccountResponse.builder()
                .message("sua conta:")
                .userId(account.getUserId())
                .balance(account.getBalance())
                .build();


    }

}
