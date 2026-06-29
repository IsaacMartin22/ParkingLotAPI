package com.example.apiservice.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParkingSpaceUpdateRequest {
    private String number;
    /** When true, removes any car currently assigned to this space. */
    private Boolean clearCar;
}

