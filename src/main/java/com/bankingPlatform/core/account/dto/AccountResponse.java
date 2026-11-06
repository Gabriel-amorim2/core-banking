package com.bankingPlatform.core.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class AccountResponse {

    private String mensage;
    private String accountHolderName;
    private String cpf;
    private BigDecimal balance;
    private String username;

}
