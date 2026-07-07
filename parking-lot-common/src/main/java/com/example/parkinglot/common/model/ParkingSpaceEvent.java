package com.example.parkinglot.common.model;

import com.example.parkinglot.common.response.ParkingSpaceResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceEvent {
    private ParkingSpaceEventType action;
    private Long lotId;
    private Long floorId;
    private Long spaceId;
    private ParkingSpaceResponse parkingSpaceResponse;
    private Instant timestamp;
}
