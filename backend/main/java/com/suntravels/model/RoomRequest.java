package com.suntravels.model;

public class RoomRequest {
    // Each entry represents one requested room and the adults assigned to that room.
    private int adults;

    public RoomRequest() {}

    public RoomRequest(int adults) {
        this.adults = adults;
    }

    public int getAdults() {
        return adults;
    }

    public void setAdults(int adults) {
        this.adults = adults;
    }
}