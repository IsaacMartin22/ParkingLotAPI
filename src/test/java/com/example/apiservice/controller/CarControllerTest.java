package com.example.apiservice.controller;

import com.example.apiservice.dbentity.Car;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.service.CarService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class CarControllerTest {

    @Test
    void updateChangesOnlyColorAndLicensePlate() {
        CarService carService = mock(CarService.class);
        CarController controller = new CarController(carService);

        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setId(9L);
        parkingSpace.setNumber("A-9");

        Car existing = new Car();
        existing.setId(42L);
        existing.setColor("Blue");
        existing.setMake("Toyota");
        existing.setModel("Corolla");
        existing.setManufacturingYear(2023);
        existing.setLicensePlate("ABC-123");
        existing.setParkingSpace(parkingSpace);

        Car request = new Car();
        request.setColor("Red");

        when(carService.findById(42L)).thenReturn(Optional.of(existing));
        when(carService.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.update(42L, request);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(42L, response.getBody().getId());
        assertEquals("Red", response.getBody().getColor());
        assertEquals("Toyota", response.getBody().getMake());
        assertEquals("Corolla", response.getBody().getModel());
        assertEquals(2023, response.getBody().getManufacturingYear());
        assertEquals("ABC-123", response.getBody().getLicensePlate());

        ArgumentCaptor<Car> captor = ArgumentCaptor.forClass(Car.class);
        verify(carService).save(captor.capture());
        Car saved = captor.getValue();
        assertSame(existing, saved);
        assertEquals("Red", saved.getColor());
        assertEquals("ABC-123", saved.getLicensePlate());
        assertEquals("Toyota", saved.getMake());
        assertEquals("Corolla", saved.getModel());
        assertEquals(2023, saved.getManufacturingYear());
        assertSame(parkingSpace, saved.getParkingSpace());
    }

    @Test
    void updateReturnsNotFoundWhenCarDoesNotExist() {
        CarService carService = mock(CarService.class);
        CarController controller = new CarController(carService);

        when(carService.findById(42L)).thenReturn(Optional.empty());

        var response = controller.update(42L, new Car());

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(carService, never()).save(any(Car.class));
    }
}

