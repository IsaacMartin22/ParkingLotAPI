package com.example.apiservice.mapper;

import com.example.apiservice.dto.LevelResponse;
import com.example.apiservice.entity.Level;
import java.util.stream.Collectors;

public class LevelMapper {
    public static LevelResponse toResponse(Level level) {
        if (level == null) {
            return null;
        }
        return new LevelResponse(
            level.getId(),
            level.getName(),
            level.getParkingLot() != null ? level.getParkingLot().getId() : null,
            level.getSections() != null 
                ? level.getSections().stream()
                    .map(section -> section.getId())
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }
}

