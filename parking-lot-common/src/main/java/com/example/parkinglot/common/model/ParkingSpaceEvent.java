package com.example.parkinglot.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceEvent {
    private com.example.parkinglot.common.model.ParkingSpaceEventType action;
    @JsonIgnore
    private Long lotId;
    @JsonIgnore
    private Long floorId;
    private Long spaceId;
    private com.example.parkinglot.common.response.ParkingSpaceResponse parkingSpaceResponse;
    @JsonIgnore
    private Instant timestamp;
}
