package com.example.apiservice.dto;

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
    private CarResponse car;
}
