package com.suntravels.model;

import java.time.LocalDate;
import java.util.List;

public class SearchRequest {
    // Represents the complete customer criteria sent by the Angular search form.
    private LocalDate checkInDate;
    private Integer noOfNights;
    private List<RoomRequest> roomRequests;

    public SearchRequest() {}

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public Integer getNoOfNights() {
        return noOfNights;
    }

    public void setNoOfNights(Integer noOfNights) {
        this.noOfNights = noOfNights;
    }

    public List<RoomRequest> getRoomRequests() {
        return roomRequests;
    }

    public void setRoomRequests(List<RoomRequest> roomRequests) {
        this.roomRequests = roomRequests;
    }
}