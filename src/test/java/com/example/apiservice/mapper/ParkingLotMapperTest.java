package com.example.apiservice.mapper;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.ParkingLot;
import com.example.apiservice.dbentity.ParkingLotType;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.pojo.ParkingLotFloorSummaryResponse;
import com.example.apiservice.pojo.ParkingLotResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParkingLotMapperTest {

    @Test
    void toResponseReturnsNullWhenParkingLotIsNull() {
        assertNull(ParkingLotMapper.toResponse(null));
    }

    @Test
    void toResponseCalculatesCapacityAndFreeSpacesAcrossFloors() {
        ParkingLot lot = new ParkingLot();
        lot.setId(101L);
        lot.setName("Downtown Garage");
        lot.setAddress("123 Main St");
        lot.setType(ParkingLotType.Economy);

        Floor floorOne = createFloor(11L, "Floor 1", List.of(
            createSection(21L, List.of(space(false), space(true))),
            createSection(22L, List.of(space(false)))
        ));

        Floor floorTwo = createFloor(12L, "Floor 2", List.of(
            createSection(23L, List.of(space(true), space(true)))
        ));

        lot.setFloors(Set.of(floorOne, floorTwo));

        ParkingLotResponse response = ParkingLotMapper.toResponse(lot);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Downtown Garage", response.getName());
        assertEquals("123 Main St", response.getAddress());
        assertEquals("Economy", response.getType());
        assertEquals(5, response.getTotalCapacity());
        assertEquals(2, response.getTotalFreeSpaces());

        List<Long> floorIds = response.getFloorIds();
        assertEquals(2, floorIds.size());
        assertEquals(Set.of(11L, 12L), Set.copyOf(floorIds));

        List<ParkingLotFloorSummaryResponse> summaries = response.getFloors();
        assertEquals(2, summaries.size());

        ParkingLotFloorSummaryResponse floorOneSummary = summaries.stream()
            .filter(summary -> summary.getId().equals(11L))
            .findFirst()
            .orElseThrow();
        assertEquals("Floor 1", floorOneSummary.getName());
        assertEquals(3, floorOneSummary.getCapacity());
        assertEquals(2, floorOneSummary.getTotalFreeSpaces());

        ParkingLotFloorSummaryResponse floorTwoSummary = summaries.stream()
            .filter(summary -> summary.getId().equals(12L))
            .findFirst()
            .orElseThrow();
        assertEquals("Floor 2", floorTwoSummary.getName());
        assertEquals(2, floorTwoSummary.getCapacity());
        assertEquals(0, floorTwoSummary.getTotalFreeSpaces());
    }

    private static Floor createFloor(Long id, String name, List<Section> sections) {
        Floor floor = new Floor();
        floor.setId(id);
        floor.setName(name);
        floor.setSections(Set.copyOf(sections));
        return floor;
    }

    private static Section createSection(Long id, List<ParkingSpace> spaces) {
        Section section = new Section();
        section.setId(id);
        section.setParkingSpaces(spaces);
        return section;
    }

    private static ParkingSpace space(boolean occupied) {
        ParkingSpace parkingSpace = new ParkingSpace();
        if (occupied) {
            parkingSpace.setLicensePlate("X");
        }
        return parkingSpace;
    }
}

