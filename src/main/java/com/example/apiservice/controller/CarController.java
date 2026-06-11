package com.example.apiservice.controller;

import com.example.apiservice.dto.CarResponse;
import com.example.apiservice.entity.Car;
import com.example.apiservice.mapper.CarMapper;
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
    public ResponseEntity<CarResponse> create(@RequestBody Car car) {
        Car saved = carService.save(car);
        return new ResponseEntity<>(CarMapper.toResponse(saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> update(@PathVariable Long id, @RequestBody Car car) {
        return carService.findById(id).map(existing -> {
            car.setId(id);
            Car updated = carService.save(car);
            return ResponseEntity.ok(CarMapper.toResponse(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

