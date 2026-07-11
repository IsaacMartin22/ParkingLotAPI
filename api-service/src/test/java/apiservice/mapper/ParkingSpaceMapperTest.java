package apiservice.mapper;

import apiservice.dbentity.ParkingSpace;
import apiservice.dbentity.Section;
import parkinglot.common.response.ParkingSpaceResponse;
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

        ParkingSpace space = new ParkingSpace();
        space.setId(31L);
        space.setNumber("A-001");
        space.setSection(section);
        space.setColor("Blue");
        space.setMake("Toyota");
        space.setModel("Corolla");
        space.setManufacturingYear(2023);
        space.setLicensePlate("ABC-123");

        ParkingSpaceResponse response = ParkingSpaceMapper.toResponse(space);

        assertNotNull(response);
        assertEquals(31L, response.getId());
        assertEquals("A-001", response.getNumber());
        assertTrue(response.isOccupied());
        assertEquals(21L, response.getSectionId());
        assertEquals("ABC-123", response.getLicensePlate());
        assertEquals("Blue", response.getColor());
    }

    @Test
    void toResponseMapsFreeParkingSpaceWithNoSection() {
        ParkingSpace space = new ParkingSpace();
        space.setId(31L);
        space.setNumber("A-001");
        space.setSection(null);
        // no car fields set for free space

        ParkingSpaceResponse response = ParkingSpaceMapper.toResponse(space);

        assertNotNull(response);
        assertEquals(31L, response.getId());
        assertEquals("A-001", response.getNumber());
        assertFalse(response.isOccupied());
        assertNull(response.getSectionId());
        assertNull(response.getColor());
        assertNull(response.getLicensePlate());
    }
}
