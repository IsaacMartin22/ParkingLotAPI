package com.example.apiservice.service;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.repository.FloorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class FloorService {

    private static final int MAX_FLOORS_PER_PARKING_LOT = 6;

    private final FloorRepository floorRepository;

    public FloorService(FloorRepository floorRepository) {
        this.floorRepository = floorRepository;
    }

    public List<Floor> getAllFloors() {
        return floorRepository.findAll();
    }

    public Optional<Floor> getFloorById(Long id) {
        return floorRepository.findById(id);
    }

    public Floor save(Floor floor) {
        validateFloorCapacity(floor);
        return floorRepository.save(floor);
    }

    public Optional<Floor> getFloorDetailsForParkingLot(Long parkingLotId, Long floorId) {
        return floorRepository.findWithSectionsAndParkingSpacesById(floorId)
                .filter(floorValue -> floorValue.getParkingLot() != null
                        && parkingLotId.equals(floorValue.getParkingLot().getId()));
    }

    private void validateFloorCapacity(Floor floor) {
        if (floor.getParkingLot() == null || floor.getParkingLot().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Floor must reference a parking lot ID");
        }

        Long targetParkingLotId = floor.getParkingLot().getId();

        if (floor.getId() != null) {
            Optional<Floor> existingFloor = floorRepository.findById(floor.getId());
            if (existingFloor.isPresent()) {
                Long currentParkingLotId = existingFloor.get().getParkingLot() == null
                        ? null
                        : existingFloor.get().getParkingLot().getId();
                if (targetParkingLotId.equals(currentParkingLotId)) {
                    return;
                }
            }
        }

        long floorCount = floorRepository.countByParkingLot_Id(targetParkingLotId);
        if (floorCount >= MAX_FLOORS_PER_PARKING_LOT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Parking lot cannot have more than " + MAX_FLOORS_PER_PARKING_LOT + " floors"
            );
        }
    }
}
