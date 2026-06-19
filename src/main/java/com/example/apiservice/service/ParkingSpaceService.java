package com.example.apiservice.service;

import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.repository.ParkingSpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ParkingSpaceService {

    private final ParkingSpaceRepository repo;

    public ParkingSpaceService(ParkingSpaceRepository repo) {
        this.repo = repo;
    }

    public List<ParkingSpace> findAll() {
        return repo.findAll();
    }

    public Optional<ParkingSpace> findById(Long id) {
        return repo.findById(id);
    }

    public ParkingSpace save(ParkingSpace space) {
        return repo.save(space);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}

