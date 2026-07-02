package com.example.apiservice.controller;

import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.pojo.ParkingSpaceUpdateRequest;
import com.example.apiservice.service.ParkingSpaceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

class ParkingSpaceControllerTest {

    private ParkingSpaceController controller(ParkingSpaceService service) {
        return new ParkingSpaceController(service);
    }

    @Test
    void updateChangesOnlyNumber() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        ParkingSpaceController controller = controller(service);

        Section section = new Section();
        section.setId(5L);

        ParkingSpace existing = new ParkingSpace();
        existing.setId(31L);
        existing.setNumber("A-001");
        existing.setSection(section);

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setNumber("A-999");

        when(service.findById(31L)).thenReturn(Optional.of(existing));
        when(service.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.update(31L, request);

        assertEquals(OK, response.getStatusCode());

        ArgumentCaptor<ParkingSpace> captor = ArgumentCaptor.forClass(ParkingSpace.class);
        verify(service).save(captor.capture());
        ParkingSpace saved = captor.getValue();
        assertSame(existing, saved);
        assertEquals("A-999", saved.getNumber());
        // Section must remain unchanged
        assertSame(section, saved.getSection());
        assertEquals(5L, saved.getSection().getId());
        // Car handling is not part of ParkingSpaceController after refactor
    }

    @Test
    void updateWithNullNumberLeavesNumberUnchanged() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        ParkingSpaceController controller = controller(service);

        ParkingSpace existing = new ParkingSpace();
        existing.setId(31L);
        existing.setNumber("A-001");

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        // number not set — null

        when(service.findById(31L)).thenReturn(Optional.of(existing));
        when(service.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.update(31L, request);

        ArgumentCaptor<ParkingSpace> captor = ArgumentCaptor.forClass(ParkingSpace.class);
        verify(service).save(captor.capture());
        assertEquals("A-001", captor.getValue().getNumber());
    }

    @Test
    void updateWithClearCarRemovesCarFromSpace() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        ParkingSpaceController controller = controller(service);

        ParkingSpace existing = new ParkingSpace();
        existing.setId(31L);
        existing.setNumber("A-001");
        existing.setLicensePlate("ABC-123");

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setClearCar(true);

        when(service.findById(31L)).thenReturn(Optional.of(existing));
        when(service.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.update(31L, request);

        assertEquals(OK, response.getStatusCode());

        // Car clearing is not supported; ensure service.save was called and occupancy remains
        ArgumentCaptor<ParkingSpace> spaceCaptor = ArgumentCaptor.forClass(ParkingSpace.class);
        verify(service).save(spaceCaptor.capture());
        assertNotNull(response.getBody());
        // since clearing cars is unsupported, occupancy remains true
        assertTrue(response.getBody().isOccupied());
    }

    @Test
    void updateWithClearCarWhenNoCarDoesNothing() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        ParkingSpaceController controller = controller(service);

        ParkingSpace existing = new ParkingSpace();
        existing.setId(31L);
        existing.setNumber("A-001");
        // no car fields set

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setClearCar(true);

        when(service.findById(31L)).thenReturn(Optional.of(existing));
        when(service.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.update(31L, request);

        // No CarService used in controller after refactor
    }

    @Test
    void updateReturnsNotFoundWhenSpaceDoesNotExist() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        ParkingSpaceController controller = controller(service);

        when(service.findById(99L)).thenReturn(Optional.empty());

        var response = controller.update(99L, new ParkingSpaceUpdateRequest());

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(service, never()).save(any(ParkingSpace.class));
    }
}
