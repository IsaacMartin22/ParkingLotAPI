package com.example.apiservice.controller;

import com.example.apiservice.pojo.ParkingSpaceResponse;
import com.example.apiservice.pojo.ParkingSpaceUpdateRequest;
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
    public ResponseEntity<ParkingSpaceResponse> update(@PathVariable Long id, @RequestBody ParkingSpaceUpdateRequest request) {
        return service.findById(id)
                .map(existing -> {
                    // Only the space number is mutable; section is locked to its original value
                    if (request.getNumber() != null) {
                        existing.setNumber(request.getNumber());
                    }
                    // Note: clearing/setting car is not supported here after refactor
                    return ResponseEntity.ok(ParkingSpaceMapper.toResponse(service.save(existing)));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
