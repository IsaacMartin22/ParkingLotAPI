package com.example.apiservice.controller;

import com.example.apiservice.dbentity.Car;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.pojo.ParkingSpaceUpdateRequest;
import com.example.apiservice.service.CarService;
import com.example.apiservice.service.ParkingSpaceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

class ParkingSpaceControllerTest {

    private ParkingSpaceController controller(ParkingSpaceService service, CarService carService) {
        return new ParkingSpaceController(service, carService);
    }

    @Test
    void updateChangesOnlyNumber() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        CarService carService = mock(CarService.class);
        ParkingSpaceController controller = controller(service, carService);

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
        // Car was not cleared
        verify(carService, never()).save(any(Car.class));
    }

    @Test
    void updateWithNullNumberLeavesNumberUnchanged() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        CarService carService = mock(CarService.class);
        ParkingSpaceController controller = controller(service, carService);

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
        CarService carService = mock(CarService.class);
        ParkingSpaceController controller = controller(service, carService);

        Car car = new Car();
        car.setId(7L);

        ParkingSpace existing = new ParkingSpace();
        existing.setId(31L);
        existing.setNumber("A-001");
        existing.setCar(car);
        car.setParkingSpace(existing);

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setClearCar(true);

        when(service.findById(31L)).thenReturn(Optional.of(existing));
        when(service.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(carService.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.update(31L, request);

        assertEquals(OK, response.getStatusCode());

        // Car's parking space reference must be nulled out (owning side update)
        ArgumentCaptor<Car> carCaptor = ArgumentCaptor.forClass(Car.class);
        verify(carService).save(carCaptor.capture());
        assertNull(carCaptor.getValue().getParkingSpace());

        // Space saved with car set to null (response should show as unoccupied)
        ArgumentCaptor<ParkingSpace> spaceCaptor = ArgumentCaptor.forClass(ParkingSpace.class);
        verify(service).save(spaceCaptor.capture());
        assertNull(spaceCaptor.getValue().getCar());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isOccupied());
    }

    @Test
    void updateWithClearCarWhenNoCarDoesNothing() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        CarService carService = mock(CarService.class);
        ParkingSpaceController controller = controller(service, carService);

        ParkingSpace existing = new ParkingSpace();
        existing.setId(31L);
        existing.setNumber("A-001");
        existing.setCar(null); // already no car

        ParkingSpaceUpdateRequest request = new ParkingSpaceUpdateRequest();
        request.setClearCar(true);

        when(service.findById(31L)).thenReturn(Optional.of(existing));
        when(service.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.update(31L, request);

        // No car to clear, so CarService should never be called
        verify(carService, never()).save(any(Car.class));
    }

    @Test
    void updateReturnsNotFoundWhenSpaceDoesNotExist() {
        ParkingSpaceService service = mock(ParkingSpaceService.class);
        CarService carService = mock(CarService.class);
        ParkingSpaceController controller = controller(service, carService);

        when(service.findById(99L)).thenReturn(Optional.empty());

        var response = controller.update(99L, new ParkingSpaceUpdateRequest());

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(service, never()).save(any(ParkingSpace.class));
        verify(carService, never()).save(any(Car.class));
    }
}
