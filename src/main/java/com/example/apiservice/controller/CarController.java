package com.example.apiservice.controller;

import com.example.apiservice.dbentity.Car;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.mapper.CarMapper;
import com.example.apiservice.pojo.CarCreateRequest;
import com.example.apiservice.pojo.CarResponse;
import com.example.apiservice.pojo.CarUpdateRequest;
import com.example.apiservice.service.CarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public List<CarResponse> getAll() {
        return carService.findAll().stream()
                .map(CarMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getById(@PathVariable Long id) {
        return carService.findById(id)
                .map(car -> ResponseEntity.ok(CarMapper.toResponse(car)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CarResponse> create(@RequestBody CarCreateRequest request) {
        Car car = new Car();
        car.setColor(request.getColor());
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setManufacturingYear(request.getManufacturingYear());
        car.setLicensePlate(request.getLicensePlate());

        if (request.getParkingSpaceId() != null) {
            ParkingSpace space = new ParkingSpace();
            space.setId(request.getParkingSpaceId());
            car.setParkingSpace(space);
        }

        Car saved = carService.save(car);
        return new ResponseEntity<>(CarMapper.toResponse(saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> update(@PathVariable Long id, @RequestBody CarUpdateRequest request) {
        return carService.findById(id)
                .map(existing -> {
                    // Only color and license plate are mutable; ID and parking space are locked
                    if (request.getColor() != null) {
                        existing.setColor(request.getColor());
                    }
                    if (request.getLicensePlate() != null) {
                        existing.setLicensePlate(request.getLicensePlate());
                    }
                    Car saved = carService.save(existing);
                    return ResponseEntity.ok(CarMapper.toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
