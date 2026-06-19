package com.example.apiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotFloorSummaryResponse {
    private Long id;
    private String name;
    private int capacity;
    private int totalFreeSpaces;
}

