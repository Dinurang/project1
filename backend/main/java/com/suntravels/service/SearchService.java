package com.suntravels.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.suntravels.model.Contract;
import com.suntravels.model.RoomRequest;
import com.suntravels.model.RoomType;
import com.suntravels.model.SearchRequest;
import com.suntravels.model.SearchResult;
import com.suntravels.repository.ContractRepository;

@Service
public class SearchService {

    private final ContractRepository contractRepository;

    public SearchService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public List<SearchResult> search(SearchRequest request) {
        List<SearchResult> results = new ArrayList<>();

        // Reject incomplete requests before touching the repository.
        if (request.getCheckInDate() == null || request.getNoOfNights() == null || request.getRoomRequests() == null) {
            return results;
        }

        LocalDate checkIn = request.getCheckInDate();
        // Contracts cover stays from check-in through the night before checkout.
        LocalDate checkOut = checkIn.plusDays(request.getNoOfNights());
        int requestedRoomCount = request.getRoomRequests().size();

        List<Contract> validContracts = contractRepository.findValidContracts(checkIn, checkOut);

        for (Contract contract : validContracts) {
            BigDecimal markupMultiplier = contract.getMarkup()
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                    .add(BigDecimal.ONE);

            for (RoomType roomType : contract.getRoomTypes()) {
                boolean allRoomsFit = roomType.getNoOfRooms() >= requestedRoomCount;
                int totalAdults = 0;

                if (allRoomsFit) {
                    for (RoomRequest roomReq : request.getRoomRequests()) {
                        if (roomReq.getAdults() > roomType.getMaxAdults()) {
                            allRoomsFit = false;
                            break;
                        }
                        totalAdults += roomReq.getAdults();
                    }
                }

                // Base rate is per adult per night * total adults * nights * (1 + markup%).
                BigDecimal totalPrice = allRoomsFit
                        ? roomType.getPrice()
                                .multiply(BigDecimal.valueOf(totalAdults))
                                .multiply(BigDecimal.valueOf(request.getNoOfNights()))
                                .multiply(markupMultiplier)
                                .setScale(2, RoundingMode.HALF_UP)
                        : null;

                results.add(new SearchResult(
                        contract.getHotelName(),
                        roomType.getTypeName(),
                        totalPrice,
                        allRoomsFit ? "Available" : "Unavailable"
                ));
            }
        }
        return results;
    }
}