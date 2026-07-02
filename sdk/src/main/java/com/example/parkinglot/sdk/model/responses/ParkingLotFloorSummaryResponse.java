package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

@Data
public class ParkingLotFloorSummaryResponse {
    private Long id;
    private String name;
    private int capacity;
    private int totalFreeSpaces;
}

