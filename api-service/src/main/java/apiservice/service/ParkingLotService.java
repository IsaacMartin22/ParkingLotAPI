package apiservice.service;

import apiservice.dbentity.ParkingLot;
import apiservice.repository.ParkingLotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ParkingLotService {

    private final ParkingLotRepository repo;

    public ParkingLotService(ParkingLotRepository repo) {
        this.repo = repo;
    }

    public List<ParkingLot> findAll() {
        return repo.findAll();
    }

    public Optional<ParkingLot> findById(Long id) {
        return repo.findById(id);
    }
}
