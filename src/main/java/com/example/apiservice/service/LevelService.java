package com.example.apiservice.service;

import com.example.apiservice.entity.Level;
import com.example.apiservice.repository.LevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LevelService {

    @Autowired
    private LevelRepository levelRepository;

    public List<Level> getAllLevels() {
        return levelRepository.findAll();
    }

    public Optional<Level> getLevelById(Long id) {
        return levelRepository.findById(id);
    }

    public Level createLevel(Level level) {
        return levelRepository.save(level);
    }

    public Level updateLevel(Long id, Level levelDetails) {
        Optional<Level> floor = levelRepository.findById(id);
        if (floor.isPresent()) {
            Level f = floor.get();
            if (levelDetails.getName() != null) {
                f.setName(levelDetails.getName());
            }
            if (levelDetails.getParkingLot() != null) {
                f.setParkingLot(levelDetails.getParkingLot());
            }
            return levelRepository.save(f);
        }
        return null;
    }

    public boolean deleteLevel(Long id) {
        if (levelRepository.existsById(id)) {
            levelRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

