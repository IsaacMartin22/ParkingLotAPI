package com.example.parkinglot.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class SectionResponse {
    private Long id;
    private String name;
    private Long floorId;
    private List<Long> parkingSpaceIds = new ArrayList<>();

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

    public Long getFloorId() {
        return floorId;
    }

    public void setFloorId(Long floorId) {
        this.floorId = floorId;
    }

    public List<Long> getParkingSpaceIds() {
        return parkingSpaceIds;
    }

    public void setParkingSpaceIds(List<Long> parkingSpaceIds) {
        this.parkingSpaceIds = parkingSpaceIds;
    }
}

