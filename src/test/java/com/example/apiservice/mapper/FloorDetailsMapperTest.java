package com.example.apiservice.mapper;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.ParkingLot;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.pojo.FloorDetailsResponse;
import com.example.apiservice.pojo.ParkingSpaceDetailsResponse;
import com.example.apiservice.pojo.SectionDetailsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloorDetailsMapperTest {

    @Test
    void toResponseReturnsNullWhenFloorIsNull() {
        assertNull(FloorDetailsMapper.toResponse(null));
    }

    @Test
    void toResponseMapsNestedSectionsAndParkingSpaces() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setId(10L);

        ParkingSpace occupiedSpace = new ParkingSpace();
        occupiedSpace.setId(31L);
        occupiedSpace.setNumber("A-001");
        // set embedded car fields directly on the parking space
        occupiedSpace.setColor("Blue");
        occupiedSpace.setMake("Toyota");
        occupiedSpace.setModel("Corolla");
        occupiedSpace.setManufacturingYear(2023);
        occupiedSpace.setLicensePlate("ABC-123");

        ParkingSpace freeSpace = new ParkingSpace();
        freeSpace.setId(32L);
        freeSpace.setNumber("A-002");

        Section section = new Section();
        section.setId(21L);
        section.setName("Section A");
        section.setParkingSpaces(List.of(occupiedSpace, freeSpace));

        Floor floor = new Floor();
        floor.setId(7L);
        floor.setName("Floor 1");
        floor.setParkingLot(parkingLot);
        floor.setSections(Set.of(section));

        FloorDetailsResponse response = FloorDetailsMapper.toResponse(floor);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("Floor 1", response.getName());
        assertEquals(10L, response.getParkingLotId());
        assertEquals(1, response.getSections().size());

        SectionDetailsResponse sectionResponse = response.getSections().get(0);
        assertEquals(21L, sectionResponse.getId());
        assertEquals("Section A", sectionResponse.getName());
        assertEquals(2, sectionResponse.getParkingSpaces().size());

        ParkingSpaceDetailsResponse occupiedSpaceResponse = sectionResponse.getParkingSpaces().stream()
            .filter(space -> space.getId().equals(31L))
            .findFirst()
            .orElseThrow();

        assertEquals("A-001", occupiedSpaceResponse.getNumber());
        assertTrue(occupiedSpaceResponse.isOccupied());
        assertEquals("Blue", occupiedSpaceResponse.getColor());
        assertEquals("Toyota", occupiedSpaceResponse.getMake());
        assertEquals("Corolla", occupiedSpaceResponse.getModel());
        assertEquals(2023, occupiedSpaceResponse.getManufacturingYear());
        assertEquals("ABC-123", occupiedSpaceResponse.getLicensePlate());

        ParkingSpaceDetailsResponse freeSpaceResponse = sectionResponse.getParkingSpaces().stream()
            .filter(space -> space.getId().equals(32L))
            .findFirst()
            .orElseThrow();

        assertEquals("A-002", freeSpaceResponse.getNumber());
        assertFalse(freeSpaceResponse.isOccupied());
        assertNull(freeSpaceResponse.getColor());
        assertNull(freeSpaceResponse.getLicensePlate());
    }

    @Test
    void toResponseHandlesNullParkingLotSectionsAndParkingSpaces() {
        Section section = new Section();
        section.setId(21L);
        section.setName("Section A");
        section.setParkingSpaces(null);

        Floor floor = new Floor();
        floor.setId(7L);
        floor.setName("Floor 1");
        floor.setParkingLot(null);
        floor.setSections(Set.of(section));

        FloorDetailsResponse response = FloorDetailsMapper.toResponse(floor);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("Floor 1", response.getName());
        assertNull(response.getParkingLotId());
        assertEquals(1, response.getSections().size());
        assertEquals(21L, response.getSections().get(0).getId());
        assertEquals("Section A", response.getSections().get(0).getName());
        assertNotNull(response.getSections().get(0).getParkingSpaces());
        assertEquals(0, response.getSections().get(0).getParkingSpaces().size());
    }

    @Test
    void toResponseHandlesNullSections() {
        Floor floor = new Floor();
        floor.setId(7L);
        floor.setName("Floor 1");
        floor.setSections(null);

        FloorDetailsResponse response = FloorDetailsMapper.toResponse(floor);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals("Floor 1", response.getName());
        assertNull(response.getParkingLotId());
        assertNotNull(response.getSections());
        assertEquals(0, response.getSections().size());
    }
}
