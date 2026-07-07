package com.example.apiservice.controller;

import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.service.ParkingSpaceService;
import com.example.parkinglot.common.request.ParkingSpaceUpdateRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class ParkingSpaceControllerTest {

    private ParkingSpaceController controller(ParkingSpaceService service) {
        return new ParkingSpaceController(service);
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
