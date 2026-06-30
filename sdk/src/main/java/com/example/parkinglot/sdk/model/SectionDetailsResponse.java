package com.example.parkinglot.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class SectionDetailsResponse {
    private Long id;
    private String name;
    private List<ParkingSpaceDetailsResponse> parkingSpaces = new ArrayList<>();

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

    public List<ParkingSpaceDetailsResponse> getParkingSpaces() {
        return parkingSpaces;
    }

    public void setParkingSpaces(List<ParkingSpaceDetailsResponse> parkingSpaces) {
        this.parkingSpaces = parkingSpaces;
    }
}

