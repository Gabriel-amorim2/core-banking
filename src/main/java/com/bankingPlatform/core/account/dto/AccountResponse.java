package com.bankingPlatform.core.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class AccountResponse {

    private String mensage;
    private UUID userId;
    private String cpf;
    private BigDecimal balance;


}
