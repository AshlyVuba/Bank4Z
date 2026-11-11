package com.bank4z.backend.fraudservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, UUID> {

    // The admin list shows the account number and amount from the linked
    // transaction. Fetching them up front avoids one extra query per row.
    @Override
    @EntityGraph(attributePaths = {"transaction", "transaction.account"})
    Page<FraudFlag> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"transaction", "transaction.account"})
    Page<FraudFlag> findByResolved(boolean resolved, Pageable pageable);
}