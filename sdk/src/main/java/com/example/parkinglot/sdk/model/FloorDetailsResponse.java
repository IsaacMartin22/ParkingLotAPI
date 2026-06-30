package com.example.parkinglot.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class FloorDetailsResponse {
    private Long id;
    private String name;
    private Long parkingLotId;
    private List<SectionDetailsResponse> sections = new ArrayList<>();

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

    public Long getParkingLotId() {
        return parkingLotId;
    }

    public void setParkingLotId(Long parkingLotId) {
        this.parkingLotId = parkingLotId;
    }

    public List<SectionDetailsResponse> getSections() {
        return sections;
    }

    public void setSections(List<SectionDetailsResponse> sections) {
        this.sections = sections;
    }
}

