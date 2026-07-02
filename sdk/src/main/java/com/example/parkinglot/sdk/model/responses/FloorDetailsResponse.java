package com.example.parkinglot.sdk.model.responses;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FloorDetailsResponse {
    private Long id;
    private String name;
    private Long parkingLotId;
    private List<SectionDetailsResponse> sections = new ArrayList<>();
}

