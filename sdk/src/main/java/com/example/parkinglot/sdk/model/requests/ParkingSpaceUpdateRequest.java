package com.example.parkinglot.sdk.model.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParkingSpaceUpdateRequest {
    private String color;
    private String make;
    private String model;
    private Integer manufacturingYear;
    private String licensePlate;
}

