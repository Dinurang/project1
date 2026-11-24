package com.suntravels.controller;

import com.suntravels.model.Contract;
import com.suntravels.model.ContractAdministrator;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.ContractRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/contracts")
// Contract-administrator APIs for loading, listing, and filtering contracts.
public class AdminContractController {

    private final ContractRepository contractRepo;
    private final ContractAdministratorRepository adminRepo;

    public AdminContractController(ContractRepository contractRepo,
                                   ContractAdministratorRepository adminRepo) {
        this.contractRepo = contractRepo;
        this.adminRepo = adminRepo;
    }

    @PostMapping
    public ResponseEntity<Contract> addContract(@RequestBody Contract contract, Authentication authentication) {
        // Bind the saved contract to the administrator identified by the JWT.
        ContractAdministrator admin = adminRepo.findByUserUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Contract Administrator profile not found."));
        contract.setAdministrator(admin);
        return ResponseEntity.ok(contractRepo.save(contract));
    }

    @GetMapping
    public List<Contract> getContracts() {
        return contractRepo.findAll();
    }

    @GetMapping("/search")
    public List<Contract> searchContracts(
            @RequestParam(required = false) String hotelName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate activeDate) {

        return contractRepo.findAll().stream()
                .filter(c -> hotelName == null || hotelName.isBlank() ||
                        c.getHotelName().toLowerCase().contains(hotelName.trim().toLowerCase()))
                // Exclude contracts whose validity period does not cover the requested date.
                .filter(c -> activeDate == null ||
                        (!c.getValidFrom().isAfter(activeDate) && !c.getValidTo().isBefore(activeDate)))
                .toList();
    }
}