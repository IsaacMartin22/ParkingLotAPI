package com.example.apiservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDetailsResponse {
    private Long id;
    private String name;
    private List<ParkingSpaceDetailsResponse> parkingSpaces;
}
