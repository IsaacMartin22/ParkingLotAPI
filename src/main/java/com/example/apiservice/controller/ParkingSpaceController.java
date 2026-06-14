package com.example.apiservice.controller;

import com.example.apiservice.dto.ParkingSpaceResponse;
import com.example.apiservice.entity.ParkingSpace;
import com.example.apiservice.mapper.ParkingSpaceMapper;
import com.example.apiservice.service.ParkingSpaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;

    public ParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParkingSpaceResponse> create(@RequestBody ParkingSpace space) {
        ParkingSpace saved = service.save(space);
        return new ResponseEntity<>(ParkingSpaceMapper.toResponse(saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> update(@PathVariable Long id, @RequestBody ParkingSpace space) {
        return service.findById(id).map(existing -> {
            space.setId(id);
            ParkingSpace updated = service.save(space);
            return ResponseEntity.ok(ParkingSpaceMapper.toResponse(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

