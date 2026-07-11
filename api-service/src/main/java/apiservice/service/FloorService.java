package apiservice.service;

import apiservice.dbentity.Floor;
import apiservice.repository.FloorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class FloorService {
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

    public Optional<Floor> getFloorDetailsForParkingLot(Long parkingLotId, Long floorId) {
        return floorRepository.findWithSectionsAndParkingSpacesById(floorId)
                .filter(floorValue -> floorValue.getParkingLot() != null
                        && parkingLotId.equals(floorValue.getParkingLot().getId()));
    }
}
