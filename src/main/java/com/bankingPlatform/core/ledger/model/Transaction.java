package com.bankingPlatform.core.ledger.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Data
@Table(name = "Ledger_transections")
public class  Transaction {

    @Id
    private UUID transactionID;

    @Column(unique = true, nullable = false)
    private String idepotencykey;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(updatable = false)
    private LocalDateTime completedAt;
}
