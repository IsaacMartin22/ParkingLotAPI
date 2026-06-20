package com.example.apiservice.service;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.repository.FloorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FloorService {

    @Autowired
    private FloorRepository floorRepository;

    public List<Floor> getAllFloors() {
        return floorRepository.findAll();
    }

    public Optional<Floor> getFloorById(Long id) {
        return floorRepository.findById(id);
    }

    public Optional<Floor> getFloorDetailsForParkingLot(Long parkingLotId, Long floorId) {
        return floorRepository.findWithSectionsAndParkingSpacesById(floorId)
                .filter(floor -> floor.getParkingLot() != null
                        && parkingLotId.equals(floor.getParkingLot().getId()));
    }

    public Floor createFloor(Floor floor) {
        return floorRepository.save(floor);
    }
}

