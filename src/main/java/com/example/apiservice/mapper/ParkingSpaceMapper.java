package com.example.apiservice.mapper;

import com.example.apiservice.pojo.ParkingSpaceResponse;
import com.example.apiservice.dbentity.ParkingSpace;

public class ParkingSpaceMapper {
    public static ParkingSpaceResponse toResponse(ParkingSpace space) {
        if (space == null) {
            return null;
        }
        return new ParkingSpaceResponse(
            space.getId(),
            space.getNumber(),
            space.getCar() != null,
            space.getSection() != null ? space.getSection().getId() : null,
            space.getCar() != null ? space.getCar().getId() : null
        );
    }
}

