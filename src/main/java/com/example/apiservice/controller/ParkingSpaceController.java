package com.example.apiservice.controller;

import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.pojo.ParkingSpaceResponse;
import com.example.apiservice.mapper.ParkingSpaceMapper;
import com.example.apiservice.service.ParkingSpaceService;
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


    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> update(@PathVariable Long id, @RequestBody ParkingSpace space) {
        return service.findById(id).map(existing -> {
            space.setId(id);
            ParkingSpace updated = service.save(space);
            return ResponseEntity.ok(ParkingSpaceMapper.toResponse(updated));
        }).orElse(ResponseEntity.notFound().build());
    }
}

