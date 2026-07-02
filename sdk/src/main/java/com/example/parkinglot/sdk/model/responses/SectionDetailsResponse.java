package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SectionDetailsResponse {
    private Long id;
    private String name;
    private List<ParkingSpaceDetailsResponse> parkingSpaces = new ArrayList<>();
}

