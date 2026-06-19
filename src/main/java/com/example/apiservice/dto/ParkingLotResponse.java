package com.example.apiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotResponse {
    private Long id;
    private String name;
    private String address;
    private int totalCapacity;
    private int totalFreeSpaces;
    private String type;
    private List<Long> floorIds;
    private List<ParkingLotFloorSummaryResponse> floors;
}

