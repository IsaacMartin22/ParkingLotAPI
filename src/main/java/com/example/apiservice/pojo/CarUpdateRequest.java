package com.example.apiservice.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CarUpdateRequest {
    private String color;
    private String licensePlate;
}

