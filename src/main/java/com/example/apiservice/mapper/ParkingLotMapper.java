package com.example.apiservice.mapper;

import com.example.apiservice.dto.ParkingLotResponse;
import com.example.apiservice.entity.ParkingLot;
import java.util.stream.Collectors;

public class ParkingLotMapper {
    public static ParkingLotResponse toResponse(ParkingLot lot) {
        if (lot == null) {
            return null;
        }
        return new ParkingLotResponse(
            lot.getId(),
            lot.getName(),
            lot.getAddress(),
            lot.getCapacity(),
            lot.getType() != null ? lot.getType().toString() : null,
            lot.getFloors() != null
                ? lot.getFloors().stream()
                    .map(level -> level.getId())
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }
}

