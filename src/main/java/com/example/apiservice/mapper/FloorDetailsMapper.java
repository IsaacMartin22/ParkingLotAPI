package com.example.apiservice.mapper;

import com.example.apiservice.dto.FloorDetailsResponse;
import com.example.apiservice.dto.ParkingSpaceDetailsResponse;
import com.example.apiservice.dto.SectionDetailsResponse;
import com.example.apiservice.entity.Floor;

import java.util.Collections;
import java.util.stream.Collectors;

public class FloorDetailsMapper {

    public static FloorDetailsResponse toResponse(Floor floor) {
        if (floor == null) {
            return null;
        }

        return new FloorDetailsResponse(
                floor.getId(),
                floor.getName(),
                floor.getParkingLot() != null ? floor.getParkingLot().getId() : null,
                floor.getSections() != null
                        ? floor.getSections().stream()
                                .map(section -> new SectionDetailsResponse(
                                        section.getId(),
                                        section.getName(),
                                        section.getParkingSpaces() != null
                                                ? section.getParkingSpaces().stream()
                                                        .map(space -> new ParkingSpaceDetailsResponse(
                                                                space.getId(),
                                                                space.getNumber(),
                                                                space.getCar() != null,
                                                                CarMapper.toResponse(space.getCar())
                                                        ))
                                                        .collect(Collectors.toList())
                                                : Collections.emptyList()
                                ))
                                .collect(Collectors.toList())
                        : Collections.emptyList()
        );
    }
}

