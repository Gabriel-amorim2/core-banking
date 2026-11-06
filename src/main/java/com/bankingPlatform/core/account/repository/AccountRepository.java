package com.bankingPlatform.core.account.repository;

import com.bankingPlatform.core.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id IN :ids")
    List<Account> findAllByIdForUpdate(@Param("ids") List<UUID> id);

    Optional<Account> findByUsername(String userId);
    Optional<Account> findByCpf(String cpf);
    void deleteByUsername(String cpf);

}
