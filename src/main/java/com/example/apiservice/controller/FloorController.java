package com.example.apiservice.controller;

import com.example.apiservice.entity.Floor;
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

    @PutMapping("/{id}")
    public ResponseEntity<Floor> updateFloor(@PathVariable Long id, @RequestBody Floor floorDetails) {
        Floor updatedFloor = floorService.updateFloor(id, floorDetails);
        if (updatedFloor != null) {
            return ResponseEntity.ok(updatedFloor);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFloor(@PathVariable Long id) {
        if (floorService.deleteFloor(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}


