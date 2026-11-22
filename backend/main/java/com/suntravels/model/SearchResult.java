package com.suntravels.model;

import java.math.BigDecimal;

public class SearchResult {
    private String hotelName;
    private String roomType;
    private BigDecimal price;
    private String status;

    public SearchResult() {}

    public SearchResult(String hotelName, String roomType, BigDecimal price, String status) {
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.price = price;
        this.status = status;
    }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}