package com.example.apiservice.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CarCreateRequest {
    private String color;
    private String make;
    private String model;
    private int manufacturingYear;
    private String licensePlate;
    private Long parkingSpaceId;
}

