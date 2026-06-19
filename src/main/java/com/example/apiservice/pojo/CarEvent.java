package com.example.apiservice.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarEvent {
    private CarEventType action;
    @JsonIgnore
    private Long lotId;
    @JsonIgnore
    private Long floorId;
    private Long spaceId;
    private CarResponse car;
    @JsonIgnore
    private Instant timestamp;
}
