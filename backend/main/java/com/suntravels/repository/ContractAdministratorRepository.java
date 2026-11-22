package com.suntravels.repository;

import com.suntravels.model.ContractAdministrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ContractAdministratorRepository extends JpaRepository<ContractAdministrator, Long> {
    Optional<ContractAdministrator> findByUserUsername(String username);
    boolean existsByEmployeeCode(String employeeCode);
}
