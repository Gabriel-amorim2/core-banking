package com.bankingPlatform.core.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name="transections_ledger")
@Immutable
public class LedgerEntry {



    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pk;
    private UUID transaction_id;
    private UUID accountId;
    @Enumerated(EnumType.STRING)
    private EntryType type; // DEBIT ou CREDIT
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
