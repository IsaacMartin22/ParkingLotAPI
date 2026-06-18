package com.example.apiservice.mapper;

import com.example.apiservice.dto.SectionResponse;
import com.example.apiservice.entity.Section;
import java.util.stream.Collectors;

public class SectionMapper {
    public static SectionResponse toResponse(Section section) {
        if (section == null) {
            return null;
        }
        Long floorId = section.getFloor() != null ? section.getFloor().getId() : null;
        return new SectionResponse(
            section.getId(),
            section.getName(),
            floorId,
            section.getParkingSpaces() != null
                ? section.getParkingSpaces().stream()
                    .map(space -> space.getId())
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }
}

