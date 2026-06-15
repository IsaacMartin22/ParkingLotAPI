package com.example.apiservice.mapper;

import com.example.apiservice.dto.LevelDetailsResponse;
import com.example.apiservice.dto.ParkingSpaceDetailsResponse;
import com.example.apiservice.dto.SectionDetailsResponse;
import com.example.apiservice.entity.Level;

import java.util.Collections;
import java.util.stream.Collectors;

public class LevelDetailsMapper {

    public static LevelDetailsResponse toResponse(Level level) {
        if (level == null) {
            return null;
        }

        return new LevelDetailsResponse(
                level.getId(),
                level.getName(),
                level.getParkingLot() != null ? level.getParkingLot().getId() : null,
                level.getSections() != null
                        ? level.getSections().stream()
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
