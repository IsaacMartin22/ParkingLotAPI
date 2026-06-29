package com.example.apiservice.controller;

import com.example.apiservice.dbentity.Car;
import com.example.apiservice.pojo.ParkingSpaceResponse;
import com.example.apiservice.pojo.ParkingSpaceUpdateRequest;
import com.example.apiservice.mapper.ParkingSpaceMapper;
import com.example.apiservice.service.CarService;
import com.example.apiservice.service.ParkingSpaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;
    private final CarService carService;

    public ParkingSpaceController(ParkingSpaceService service, CarService carService) {
        this.service = service;
        this.carService = carService;
    }

    @GetMapping
    public List<ParkingSpaceResponse> getAll() {
        return service.findAll().stream()
                .map(ParkingSpaceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(space -> ResponseEntity.ok(ParkingSpaceMapper.toResponse(space)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> update(@PathVariable Long id, @RequestBody ParkingSpaceUpdateRequest request) {
        return service.findById(id)
                .map(existing -> {
                    // Only the space number is mutable; section is locked to its original value
                    if (request.getNumber() != null) {
                        existing.setNumber(request.getNumber());
                    }
                    // Disassociate the car if requested; must update via Car (the owning side)
                    if (Boolean.TRUE.equals(request.getClearCar()) && existing.getCar() != null) {
                        Car car = existing.getCar();
                        car.setParkingSpace(null);
                        carService.save(car);
                        existing.setCar(null); // keep in-memory state consistent for the response
                    }
                    return ResponseEntity.ok(ParkingSpaceMapper.toResponse(service.save(existing)));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
