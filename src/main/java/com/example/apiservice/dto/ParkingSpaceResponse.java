package com.example.apiservice.dto;

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
    private Long carId;
}

