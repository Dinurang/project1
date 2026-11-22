package com.suntravels.repository;

import com.suntravels.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    // The contract must cover both the check-in date and the final stay date.
    @Query("SELECT c FROM Contract c WHERE c.validFrom <= :checkIn AND c.validTo >= :checkOut")
    List<Contract> findValidContracts(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);
}