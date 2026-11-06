package com.bankingPlatform.core.ledger.repository;

import com.bankingPlatform.core.ledger.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

}
