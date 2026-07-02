package com.example.apiservice.mapper;

import com.example.apiservice.pojo.ParkingSpaceResponse;
import com.example.apiservice.dbentity.ParkingSpace;

public class ParkingSpaceMapper {
    public static ParkingSpaceResponse toResponse(ParkingSpace space) {
        if (space == null) {
            return null;
        }
        boolean occupied = space.getLicensePlate() != null;
        return new ParkingSpaceResponse(
            space.getId(),
            space.getNumber(),
            occupied,
            space.getSection() != null ? space.getSection().getId() : null,
            space.getColor(),
            space.getMake(),
            space.getModel(),
            space.getManufacturingYear() == null ? 0 : space.getManufacturingYear(),
            space.getLicensePlate()
        );
    }
}

