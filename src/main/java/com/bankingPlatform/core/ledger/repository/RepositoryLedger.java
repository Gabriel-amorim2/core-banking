package com.bankingPlatform.core.ledger.repository;

import com.bankingPlatform.core.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RepositoryLedger extends JpaRepository<LedgerEntry, UUID> {



    }
