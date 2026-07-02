package com.example.apiservice.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceEvent {
    private ParkingSpaceEventType action;
    @JsonIgnore
    private Long lotId;
    @JsonIgnore
    private Long floorId;
    private Long spaceId;
    private ParkingSpaceResponse parkingSpaceResponse;
    @JsonIgnore
    private Instant timestamp;
}
