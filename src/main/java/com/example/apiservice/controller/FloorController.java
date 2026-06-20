package com.example.apiservice.controller;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.service.FloorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/floors")
public class FloorController {

    @Autowired
    private FloorService floorService;

    @GetMapping
    public List<Floor> getAllFloors() {
        return floorService.getAllFloors();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Floor> getFloorById(@PathVariable Long id) {
        Optional<Floor> floor = floorService.getFloorById(id);
        return floor.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Floor> createFloor(@RequestBody Floor floor) {
        Floor createdFloor = floorService.createFloor(floor);
        return ResponseEntity.ok(createdFloor);
    }
}


