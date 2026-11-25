package com.suntravels.controller;

import com.suntravels.model.Contract;
import com.suntravels.model.ContractAdministrator;
import com.suntravels.model.RoomType;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.ContractRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/contracts")
// Contract-administrator APIs for creating, managing, listing, and filtering contracts.
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

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Contract> updateContract(@PathVariable Long id, @RequestBody Contract updatedContract) {
        Contract contract = contractRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found."));
        if (updatedContract.getRoomTypes() == null || updatedContract.getRoomTypes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one room type is required.");
        }

        contract.setHotelName(updatedContract.getHotelName());
        contract.setValidFrom(updatedContract.getValidFrom());
        contract.setValidTo(updatedContract.getValidTo());
        contract.setMarkup(updatedContract.getMarkup());
        contract.getRoomTypes().clear();
        for (RoomType updatedRoomType : updatedContract.getRoomTypes()) {
            RoomType roomType = new RoomType();
            roomType.setTypeName(updatedRoomType.getTypeName());
            roomType.setPrice(updatedRoomType.getPrice());
            roomType.setNoOfRooms(updatedRoomType.getNoOfRooms());
            roomType.setMaxAdults(updatedRoomType.getMaxAdults());
            contract.getRoomTypes().add(roomType);
        }

        return ResponseEntity.ok(contractRepo.save(contract));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        Contract contract = contractRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found."));
        contractRepo.delete(contract);
        return ResponseEntity.noContent().build();
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