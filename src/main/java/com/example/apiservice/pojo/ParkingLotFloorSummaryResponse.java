package com.example.apiservice.pojo;

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

