package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

@Data
public class ParkingSpaceResponse {
    private Long id;
    private String number;
    private boolean occupied;
    private Long sectionId;
    private Long carId;
}

