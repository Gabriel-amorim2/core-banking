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

    private String message;
    private UUID userId;
    private BigDecimal balance;


}
