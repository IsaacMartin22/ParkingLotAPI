package apiservice.mapper;

import apiservice.dbentity.Floor;
import apiservice.dbentity.ParkingSpace;
import apiservice.dbentity.Section;
import parkinglot.common.response.SectionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SectionMapperTest {

    @Test
    void toResponseReturnsNullWhenSectionIsNull() {
        assertNull(SectionMapper.toResponse(null));
    }

    @Test
    void toResponseMapsAllFields() {
        Floor floor = new Floor();
        floor.setId(11L);

        ParkingSpace spaceOne = new ParkingSpace();
        spaceOne.setId(31L);

        ParkingSpace spaceTwo = new ParkingSpace();
        spaceTwo.setId(32L);

        Section section = new Section();
        section.setId(21L);
        section.setName("Section A");
        section.setFloor(floor);
        section.setParkingSpaces(List.of(spaceOne, spaceTwo));

        SectionResponse response = SectionMapper.toResponse(section);

        assertNotNull(response);
        assertEquals(21L, response.getId());
        assertEquals("Section A", response.getName());
        assertEquals(11L, response.getFloorId());
        assertEquals(2, response.getParkingSpaceIds().size());
        assertEquals(Set.of(31L, 32L), Set.copyOf(response.getParkingSpaceIds()));
    }

    @Test
    void toResponseHandlesNullFloorAndParkingSpaces() {
        Section section = new Section();
        section.setId(21L);
        section.setName("Section A");
        section.setFloor(null);
        section.setParkingSpaces(null);

        SectionResponse response = SectionMapper.toResponse(section);

        assertNotNull(response);
        assertEquals(21L, response.getId());
        assertEquals("Section A", response.getName());
        assertNull(response.getFloorId());
        assertNotNull(response.getParkingSpaceIds());
        assertEquals(0, response.getParkingSpaceIds().size());
    }
}
