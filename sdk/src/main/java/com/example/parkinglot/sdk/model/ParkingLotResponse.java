package com.example.parkinglot.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class ParkingLotResponse {
    private Long id;
    private String name;
    private String address;
    private String type;
    private int totalCapacity;
    private int totalFreeSpaces;
    private List<Long> floorIds = new ArrayList<>();
    private List<ParkingLotFloorSummaryResponse> floors = new ArrayList<>();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getTotalFreeSpaces() {
        return totalFreeSpaces;
    }

    public void setTotalFreeSpaces(int totalFreeSpaces) {
        this.totalFreeSpaces = totalFreeSpaces;
    }

    public List<Long> getFloorIds() {
        return floorIds;
    }

    public void setFloorIds(List<Long> floorIds) {
        this.floorIds = floorIds;
    }

    public List<ParkingLotFloorSummaryResponse> getFloors() {
        return floors;
    }

    public void setFloors(List<ParkingLotFloorSummaryResponse> floors) {
        this.floors = floors;
    }
}

