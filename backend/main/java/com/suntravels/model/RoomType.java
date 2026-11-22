package com.suntravels.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "room_type")
// Defines the hoteler's base price and inventory for one room category.
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "no_of_rooms", nullable = false)
    private Integer noOfRooms;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    public RoomType() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getNoOfRooms() { return noOfRooms; }
    public void setNoOfRooms(Integer noOfRooms) { this.noOfRooms = noOfRooms; }

    public Integer getMaxAdults() { return maxAdults; }
    public void setMaxAdults(Integer maxAdults) { this.maxAdults = maxAdults; }
}