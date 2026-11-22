package com.suntravels.model;

public class RoomRequest {
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