package com.example.apiservice.mapper;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.Section;
import com.example.parkinglot.common.response.FloorResponse;
import java.util.stream.Collectors;

public class FloorMapper {
    public static FloorResponse toResponse(Floor floor) {
        if (floor == null) {
            return null;
        }
        return new FloorResponse(
            floor.getId(),
            floor.getName(),
            floor.getParkingLot() != null ? floor.getParkingLot().getId() : null,
            floor.getSections() != null
                ? floor.getSections().stream()
                    .map(Section::getId)
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }
}


