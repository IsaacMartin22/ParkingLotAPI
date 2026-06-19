package com.example.apiservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {
    private Long id;
    private String color;
    private String make;
    private String model;
    private int manufacturingYear;
    private String licensePlate;
}

