package com.example.apiservice.controller;

import com.example.apiservice.entity.ParkingLot;
import com.example.apiservice.service.ParkingLotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
public class ParkingLotController {

    private final ParkingLotService service;

    public ParkingLotController(ParkingLotService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParkingLot> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingLot> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ParkingLot> create(@RequestBody ParkingLot lot) {
        ParkingLot saved = service.save(lot);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingLot> update(@PathVariable Long id, @RequestBody ParkingLot lot) {
        return service.findById(id).map(existing -> {
            lot.setId(id);
            ParkingLot updated = service.save(lot);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

