package com.example.parkinglot.sdk.model;

public class ParkingLotFloorSummaryResponse {
    private Long id;
    private String name;
    private int capacity;
    private int totalFreeSpaces;

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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getTotalFreeSpaces() {
        return totalFreeSpaces;
    }

    public void setTotalFreeSpaces(int totalFreeSpaces) {
        this.totalFreeSpaces = totalFreeSpaces;
    }
}

