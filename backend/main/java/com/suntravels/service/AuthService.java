package com.suntravels.service;

import com.suntravels.dto.AuthResponse;
import com.suntravels.dto.LoginRequest;
import com.suntravels.dto.SignupRequest;
import com.suntravels.model.*;
import com.suntravels.repository.CallCenterAgentRepository;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ContractAdministratorRepository adminRepository;
    private final CallCenterAgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ContractAdministratorRepository adminRepository,
                       CallCenterAgentRepository agentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void signup(SignupRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        if (req.getRole() == null) {
            throw new IllegalArgumentException("User role must be specified.");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        User savedUser = userRepository.save(user);

        if (req.getRole() == Role.CONTRACT_ADMIN) {
            if (req.getEmployeeCode() == null || req.getEmployeeCode().isBlank()) {
                throw new IllegalArgumentException("Employee code is required for Contract Administrator.");
            }
            if (adminRepository.existsByEmployeeCode(req.getEmployeeCode().trim())) {
                throw new IllegalArgumentException("Employee code already exists.");
            }
            ContractAdministrator admin = new ContractAdministrator();
            admin.setUser(savedUser);
            admin.setFullName(req.getFullName().trim());
            admin.setEmployeeCode(req.getEmployeeCode().trim());
            admin.setContactNo(req.getContactNo() != null ? req.getContactNo().trim() : "");
            adminRepository.save(admin);

        } else if (req.getRole() == Role.CALL_CENTER_AGENT) {
            if (req.getAgentCode() == null || req.getAgentCode().isBlank()) {
                throw new IllegalArgumentException("Agent code is required for Call Center Agent.");
            }
            if (agentRepository.existsByAgentCode(req.getAgentCode().trim())) {
                throw new IllegalArgumentException("Agent code already exists.");
            }
            CallCenterAgent agent = new CallCenterAgent();
            agent.setUser(savedUser);
            agent.setFullName(req.getFullName().trim());
            agent.setAgentCode(req.getAgentCode().trim());
            agent.setDeskExtension(req.getDeskExtension() != null ? req.getDeskExtension().trim() : "");
            agentRepository.save(agent);
        }
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String fullName = user.getUsername();
        if (user.getRole() == Role.CONTRACT_ADMIN) {
            fullName = adminRepository.findByUserUsername(user.getUsername())
                    .map(ContractAdministrator::getFullName)
                    .orElse(user.getUsername());
        } else if (user.getRole() == Role.CALL_CENTER_AGENT) {
            fullName = agentRepository.findByUserUsername(user.getUsername())
                    .map(CallCenterAgent::getFullName)
                    .orElse(user.getUsername());
        }

        String token = jwtService.generateToken(user, fullName);
        return new AuthResponse(token, user.getUsername(), fullName, user.getRole());
    }
}
