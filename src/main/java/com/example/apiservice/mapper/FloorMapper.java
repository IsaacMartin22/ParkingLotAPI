package com.example.apiservice.mapper;

import com.example.apiservice.dto.FloorResponse;
import com.example.apiservice.entity.Floor;
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
                    .map(section -> section.getId())
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }
}


