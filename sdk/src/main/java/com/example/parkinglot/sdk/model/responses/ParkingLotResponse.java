package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParkingLotResponse {
    private Long id;
    private String name;
    private String address;
    private String type;
    private int totalCapacity;
    private int totalFreeSpaces;
    private List<Long> floorIds = new ArrayList<>();
    private List<ParkingLotFloorSummaryResponse> floors = new ArrayList<>();
}

