package com.example.apiservice.mapper;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.ParkingLot;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.pojo.FloorResponse;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FloorMapperTest {

    @Test
    void toResponseReturnsNullWhenFloorIsNull() {
        assertNull(FloorMapper.toResponse(null));
    }

    @Test
    void toResponseMapsAllFields() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setId(10L);

        Section sectionOne = new Section();
        sectionOne.setId(21L);

        Section sectionTwo = new Section();
        sectionTwo.setId(22L);

        Floor floor = new Floor();
        floor.setId(7L);
        floor.setName("Floor 1");
        floor.setParkingLot(parkingLot);
        floor.setSections(Set.of(sectionOne, sectionTwo));

        FloorResponse response = FloorMapper.toResponse(floor);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("Floor 1", response.getName());
        assertEquals(10L, response.getParkingLotId());
        assertEquals(2, response.getSectionIds().size());
        assertEquals(Set.of(21L, 22L), Set.copyOf(response.getSectionIds()));
    }

    @Test
    void toResponseHandlesNullParkingLotAndSections() {
        Floor floor = new Floor();
        floor.setId(7L);
        floor.setName("Floor 1");
        floor.setParkingLot(null);
        floor.setSections(null);

        FloorResponse response = FloorMapper.toResponse(floor);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("Floor 1", response.getName());
        assertNull(response.getParkingLotId());
        assertNotNull(response.getSectionIds());
        assertEquals(0, response.getSectionIds().size());
    }
}
