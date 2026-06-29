package com.example.apiservice.controller;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.mapper.FloorMapper;
import com.example.apiservice.pojo.FloorResponse;
import com.example.apiservice.service.FloorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/floors")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping
    public List<FloorResponse> getAllFloors() {
        return floorService.getAllFloors().stream()
                .map(FloorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FloorResponse> getFloorById(@PathVariable Long id) {
        return floorService.getFloorById(id)
                .map(floor -> ResponseEntity.ok(FloorMapper.toResponse(floor)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FloorResponse> createFloor(@RequestBody Floor floor) {
        Floor createdFloor = floorService.createFloor(floor);
        return new ResponseEntity<>(FloorMapper.toResponse(createdFloor), HttpStatus.CREATED);
    }
}
