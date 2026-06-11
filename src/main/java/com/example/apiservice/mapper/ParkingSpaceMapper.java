package com.example.apiservice.mapper;

import com.example.apiservice.dto.ParkingSpaceResponse;
import com.example.apiservice.entity.ParkingSpace;

public class ParkingSpaceMapper {
    public static ParkingSpaceResponse toResponse(ParkingSpace space) {
        if (space == null) {
            return null;
        }
        return new ParkingSpaceResponse(
            space.getId(),
            space.getNumber(),
            space.isOccupied(),
            space.getSection() != null ? space.getSection().getId() : null,
            space.getCar() != null ? space.getCar().getId() : null
        );
    }
}

