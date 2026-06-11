package com.example.apiservice.mapper;

import com.example.apiservice.dto.SectionResponse;
import com.example.apiservice.entity.Section;
import java.util.stream.Collectors;

public class SectionMapper {
    public static SectionResponse toResponse(Section section) {
        if (section == null) {
            return null;
        }
        return new SectionResponse(
            section.getId(),
            section.getName(),
            section.getLevel() != null ? section.getLevel().getId() : null,
            section.getParkingSpaces() != null 
                ? section.getParkingSpaces().stream()
                    .map(space -> space.getId())
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }
}

