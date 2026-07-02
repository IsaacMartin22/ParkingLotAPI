package com.example.apiservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceResponse {
    private Long id;
    private String number;
    private boolean occupied;
    private Long sectionId;
    private String color;
    private String make;
    private String model;
    private int manufacturingYear;
    private String licensePlate;
}

