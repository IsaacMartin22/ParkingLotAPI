package com.example.apiservice.controller;

import com.example.apiservice.entity.Level;
import com.example.apiservice.service.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/levels")
public class LevelController {

    @Autowired
    private LevelService levelService;

    @GetMapping
    public List<Level> getAllFloors() {
        return levelService.getAllLevels();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Level> getLevelById(@PathVariable Long id) {
        Optional<Level> level = levelService.getLevelById(id);
        return level.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Level> createLevel(@RequestBody Level level) {
        Level createdLevel = levelService.createLevel(level);
        return ResponseEntity.ok(createdLevel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Level> updateLevel(@PathVariable Long id, @RequestBody Level levelDetails) {
        Level updatedLevel = levelService.updateLevel(id, levelDetails);
        if (updatedLevel != null) {
            return ResponseEntity.ok(updatedLevel);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLevel(@PathVariable Long id) {
        if (levelService.deleteLevel(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

