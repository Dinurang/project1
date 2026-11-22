package com.suntravels.repository;

import com.suntravels.model.CallCenterAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CallCenterAgentRepository extends JpaRepository<CallCenterAgent, Long> {
    Optional<CallCenterAgent> findByUserUsername(String username);
    boolean existsByAgentCode(String agentCode);
}
