package com.example.apiservice.controller;

import com.example.apiservice.entity.ParkingSpace;
import com.example.apiservice.service.ParkingSpaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;

    public ParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParkingSpace> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpace> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ParkingSpace> create(@RequestBody ParkingSpace space) {
        ParkingSpace saved = service.save(space);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpace> update(@PathVariable Long id, @RequestBody ParkingSpace space) {
        return service.findById(id).map(existing -> {
            space.setId(id);
            ParkingSpace updated = service.save(space);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

