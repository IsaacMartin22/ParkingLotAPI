package com.example.apiservice.mapper;

import com.example.apiservice.dbentity.Car;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.pojo.ParkingSpaceResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingSpaceMapperTest {

    @Test
    void toResponseReturnsNullWhenParkingSpaceIsNull() {
        assertNull(ParkingSpaceMapper.toResponse(null));
    }

    @Test
    void toResponseMapsOccupiedParkingSpace() {
        Section section = new Section();
        section.setId(21L);

        Car car = new Car();
        car.setId(41L);

        ParkingSpace space = new ParkingSpace();
        space.setId(31L);
        space.setNumber("A-001");
        space.setSection(section);
        space.setCar(car);

        ParkingSpaceResponse response = ParkingSpaceMapper.toResponse(space);

        assertNotNull(response);
        assertEquals(31L, response.getId());
        assertEquals("A-001", response.getNumber());
        assertTrue(response.isOccupied());
        assertEquals(21L, response.getSectionId());
        assertEquals(41L, response.getCarId());
    }

    @Test
    void toResponseMapsFreeParkingSpaceWithNoSection() {
        ParkingSpace space = new ParkingSpace();
        space.setId(31L);
        space.setNumber("A-001");
        space.setSection(null);
        space.setCar(null);

        ParkingSpaceResponse response = ParkingSpaceMapper.toResponse(space);

        assertNotNull(response);
        assertEquals(31L, response.getId());
        assertEquals("A-001", response.getNumber());
        assertFalse(response.isOccupied());
        assertNull(response.getSectionId());
        assertNull(response.getCarId());
    }
}
