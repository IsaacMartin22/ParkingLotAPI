package com.example.apiservice.mapper;

import com.example.apiservice.pojo.FloorDetailsResponse;
import com.example.apiservice.pojo.ParkingSpaceDetailsResponse;
import com.example.apiservice.pojo.SectionDetailsResponse;
import com.example.apiservice.dbentity.Floor;

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
                                                        .map(space -> {
                                                                ParkingSpaceDetailsResponse ps = new ParkingSpaceDetailsResponse();
                                                                ps.setId(space.getId());
                                                                ps.setNumber(space.getNumber());
                                                                ps.setOccupied(space.getLicensePlate() != null);
                                                                ps.setColor(space.getColor());
                                                                ps.setMake(space.getMake());
                                                                ps.setModel(space.getModel());
                                                                ps.setManufacturingYear(space.getManufacturingYear() == null ? 0 : space.getManufacturingYear());
                                                                ps.setLicensePlate(space.getLicensePlate());
                                                                return ps;
                                                        })
                                                        .collect(Collectors.toList())
                                                : Collections.emptyList()
                                ))
                                .collect(Collectors.toList())
                        : Collections.emptyList()
        );
    }
}

