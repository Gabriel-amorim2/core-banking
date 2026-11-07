package com.bankingPlatform.core.ledger.service;

import com.bankingPlatform.core.account.entity.Account;
import com.bankingPlatform.core.account.repository.AccountRepository;
import com.bankingPlatform.core.ledger.model.*;
import com.bankingPlatform.core.ledger.repository.RepositoryLedger;
import com.bankingPlatform.core.ledger.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TransferService {
    @Autowired
    private AccountRepository aRepository;
    @Autowired
    private TransactionRepository tRepository;
    @Autowired
    private RepositoryLedger lRepository;


    @Transactional
    public void transfer(UUID from, UUID to, String type, BigDecimal amount, String idempotencyKey){


        List<UUID> ids = Stream.of(from, to)
                .sorted()
                .toList();

        List<Account> accounts = aRepository.findAllByIdForUpdate(ids);

        Map<UUID, Account> accountMap = accounts.stream()
                .collect(Collectors.toMap(Account::getId, acc -> acc));

        Account origin = accountMap.get(from);
        Account destination = accountMap.get(to);

        if (origin == null||destination == null){
        throw new RuntimeException("account not found");
        }

        if (origin.getBalance().compareTo(amount)<0){
            throw new RuntimeException("insufficient balance");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionID(UUID.randomUUID());
        transaction.setType(TransactionType.valueOf(type));
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);

        LedgerEntry debit =buildEntry(transaction.getTransactionID(),from,EntryType.DEBIT, amount);
        LedgerEntry credit= buildEntry(transaction.getTransactionID(),to,EntryType.CREDIT, amount);

        post(transaction,List.of(debit,credit));
    }

    private void post(Transaction transaction, List<LedgerEntry> entries){
        try {
            tRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            return;
        }

        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Entries cannot be empty");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {

            if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid amount");
            }

            if (entry.getType() == EntryType.DEBIT) {
                totalDebit = totalDebit.add(entry.getAmount());
            } else if (entry.getType() == EntryType.CREDIT) {
                totalCredit = totalCredit.add(entry.getAmount());
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalStateException("Unbalanced transaction");
        }

        for (LedgerEntry entry : entries) {
            entry.setCreatedAt(LocalDateTime.now());
        }

        lRepository.saveAll(entries);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());

        tRepository.save(transaction);
    }
    


    private LedgerEntry buildEntry(UUID txId, UUID accountId, EntryType type, BigDecimal amount) {

        LedgerEntry entry = new LedgerEntry();
        entry.setTransaction_id(txId);
        entry.setAccountId(accountId);
        entry.setType(type);
        entry.setAmount(amount);

        return entry;
    }


}
