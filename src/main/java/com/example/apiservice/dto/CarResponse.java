package com.example.apiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {
    private Long id;
    private String make;
    private String model;
    private int manufacturingYear;
    private String licensePlate;
    private boolean parked;
    private Long parkingSpaceId;
}

