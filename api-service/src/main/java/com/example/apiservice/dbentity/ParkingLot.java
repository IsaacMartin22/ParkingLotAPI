package com.example.apiservice.dbentity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parking_lots")
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;

    @Enumerated(EnumType.STRING)
    private ParkingLotType type;

    @OneToMany(mappedBy = "parkingLot", cascade = CascadeType.ALL)
    @JsonBackReference
    @Size(max = 6, message = "Parking lot cannot have more than 6 floors")
    private Set<Floor> floors = new HashSet<>();

    public ParkingLot() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public ParkingLotType getType() {
        return type;
    }

    public void setType(ParkingLotType type) {
        this.type = type;
    }

    public Set<Floor> getFloors() {
        return floors;
    }

    public void setFloors(Set<Floor> floors) {
        if (floors != null && floors.size() > 6) {
            throw new IllegalArgumentException("Parking lot cannot have more than 6 floors");
        }
        this.floors = floors;
    }
}
