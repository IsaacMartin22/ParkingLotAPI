package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

@Data
public class ParkingSpaceDetailsResponse {
    private Long id;
    private String number;
    private boolean occupied;
    private String color;
    private String make;
    private String model;
    private int manufacturingYear;
    private String licensePlate;
}

