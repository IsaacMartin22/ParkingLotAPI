package com.example.apiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelDetailsResponse {
    private Long id;
    private String name;
    private Long parkingLotId;
    private List<SectionDetailsResponse> sections;
}
