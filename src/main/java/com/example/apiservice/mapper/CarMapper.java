package com.example.apiservice.mapper;

import com.example.apiservice.pojo.CarResponse;
import com.example.apiservice.dbentity.Car;

public class CarMapper {
    public static CarResponse toResponse(Car car) {
        if (car == null) {
            return null;
        }
        return new CarResponse(
            car.getId(),
            car.getColor(),
            car.getMake(),
            car.getModel(),
            car.getManufacturingYear(),
            car.getLicensePlate()
        );
    }
}

