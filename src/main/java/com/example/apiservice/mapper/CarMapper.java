package com.example.apiservice.mapper;

import com.example.apiservice.dto.CarResponse;
import com.example.apiservice.entity.Car;

public class CarMapper {
    public static CarResponse toResponse(Car car) {
        if (car == null) {
            return null;
        }
        return new CarResponse(
            car.getId(),
            car.getMake(),
            car.getModel(),
            car.getManufacturingYear(),
            car.getLicensePlate(),
            car.isParked(),
            car.getParkingSpace() != null ? car.getParkingSpace().getId() : null
        );
    }
}

