package com.bankingPlatform.core.account.controller;


import com.bankingPlatform.core.account.dto.AccountRequest;
import com.bankingPlatform.core.account.dto.AccountResponse;

import com.bankingPlatform.core.account.service.AccountServiceImp;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {


    private final AccountServiceImp accountService;

    public AccountController(AccountServiceImp accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/create-account")
    public ResponseEntity<AccountResponse> addAccount(@Valid @RequestBody AccountRequest accountRequest){
        var count =  accountService.createAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(count);
    }
    @DeleteMapping("/delete-account")
    public ResponseEntity<AccountResponse> deleteByCpf(@RequestBody UUID userId){
        var delete = accountService.deleteAccounts(userId);
        return ResponseEntity.status(HttpStatus.OK).body(delete);
    }

    @GetMapping("/get-account")
    public ResponseEntity<AccountResponse> getAccount (UUID userId){
        var account = accountService.getMyAccounts(userId);
        return ResponseEntity.status(HttpStatus.OK).body(account);
    }
}
