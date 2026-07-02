package com.example.apiservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
