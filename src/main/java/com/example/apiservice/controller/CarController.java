package com.example.apiservice.controller;

import com.example.apiservice.entity.Car;
import com.example.apiservice.service.CarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public List<Car> getAll() {
        return carService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Car> getById(@PathVariable Long id) {
        return carService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Car> create(@RequestBody Car car) {
        Car saved = carService.save(car);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Car> update(@PathVariable Long id, @RequestBody Car car) {
        return carService.findById(id).map(existing -> {
            car.setId(id);
            Car updated = carService.save(car);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

