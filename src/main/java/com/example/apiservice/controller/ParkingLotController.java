package com.example.apiservice.controller;

import com.example.apiservice.pojo.FloorDetailsResponse;
import com.example.apiservice.pojo.ParkingLotResponse;
import com.example.apiservice.mapper.FloorDetailsMapper;
import com.example.apiservice.mapper.ParkingLotMapper;
import com.example.apiservice.service.FloorService;
import com.example.apiservice.service.ParkingLotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lots")
public class ParkingLotController {

    private final ParkingLotService service;
    private final FloorService floorService;

    public ParkingLotController(ParkingLotService service, FloorService floorService) {
        this.service = service;
        this.floorService = floorService;
    }

    @GetMapping
    public List<ParkingLotResponse> getAll() {
        return service.findAll().stream()
                .map(ParkingLotMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingLotResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(lot -> ResponseEntity.ok(ParkingLotMapper.toResponse(lot)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{lotId}/floors/{floorId}/details")
    public ResponseEntity<FloorDetailsResponse> getFloorDetailsForParkingLot(
            @PathVariable Long lotId,
            @PathVariable Long floorId
    ) {
        return floorService.getFloorDetailsForParkingLot(lotId, floorId)
                .map(floor -> ResponseEntity.ok(FloorDetailsMapper.toResponse(floor)))
                .orElse(ResponseEntity.notFound().build());
    }
}

