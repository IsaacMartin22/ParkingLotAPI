package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SectionResponse {
    private Long id;
    private String name;
    private Long floorId;
    private List<Long> parkingSpaceIds = new ArrayList<>();
}

