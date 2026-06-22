package com.example.apiservice.mapper;

import com.example.apiservice.dbentity.Car;
import com.example.apiservice.pojo.CarResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CarMapperTest {

    @Test
    void toResponseReturnsNullWhenCarIsNull() {
        assertNull(CarMapper.toResponse(null));
    }

    @Test
    void toResponseMapsAllFields() {
        Car car = new Car();
        car.setId(7L);
        car.setColor("Blue");
        car.setMake("Toyota");
        car.setModel("Corolla");
        car.setManufacturingYear(2023);
        car.setLicensePlate("ABC-123");

        CarResponse response = CarMapper.toResponse(car);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("Blue", response.getColor());
        assertEquals("Toyota", response.getMake());
        assertEquals("Corolla", response.getModel());
        assertEquals(2023, response.getManufacturingYear());
        assertEquals("ABC-123", response.getLicensePlate());
    }
}

