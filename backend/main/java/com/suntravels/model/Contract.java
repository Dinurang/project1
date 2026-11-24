package com.suntravels.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract")
// A hotel contract may contain several room types and belongs to one administrator.
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
    private ContractAdministrator administrator;

    @Column(name = "hotel_name", nullable = false)
    private String hotelName;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal markup;

    // Saving or removing a contract also saves or removes its associated room types.
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contract_id", nullable = false)
    private List<RoomType> roomTypes = new ArrayList<>();

    public Contract() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ContractAdministrator getAdministrator() { return administrator; }
    public void setAdministrator(ContractAdministrator administrator) { this.administrator = administrator; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public BigDecimal getMarkup() { return markup; }
    public void setMarkup(BigDecimal markup) { this.markup = markup; }

    public List<RoomType> getRoomTypes() { return roomTypes; }
    public void setRoomTypes(List<RoomType> roomTypes) { this.roomTypes = roomTypes; }
}