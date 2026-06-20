package com.example.apiservice.service;

import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.ParkingLot;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.mapper.CarMapper;
import com.example.apiservice.pojo.CarEvent;
import com.example.apiservice.pojo.CarEventType;
import com.example.apiservice.pojo.CarResponse;
import com.example.apiservice.repository.ParkingSpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ParkingSpaceService {

    private final ParkingSpaceRepository repo;
    private final FloorEventService floorEventService;

    public ParkingSpaceService(ParkingSpaceRepository repo, FloorEventService floorEventService) {
        this.repo = repo;
        this.floorEventService = floorEventService;
    }

    public List<ParkingSpace> findAll() {
        return repo.findAll();
    }

    public Optional<ParkingSpace> findById(Long id) {
        return repo.findById(id);
    }

    public ParkingSpace save(ParkingSpace space) {
        boolean isUpdate = space.getId() != null && repo.existsById(space.getId());
        ParkingSpace saved = repo.save(space);

        if (!isUpdate) {
            return saved;
        }

        ParkingSpace fullSpace = repo.findById(saved.getId()).orElse(saved);
        resolveFloorContext(fullSpace).ifPresent(ctx -> {
            CarResponse carResponse = CarMapper.toResponse(fullSpace.getCar());
            CarEvent event = new CarEvent(
                    CarEventType.UPDATE,
                    ctx.lotId,
                    ctx.floorId,
                    fullSpace.getId(),
                    carResponse,
                    Instant.now()
            );
            floorEventService.publishEvent(ctx.lotId, ctx.floorId, event);
        });

        return saved;
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private record FloorContext(Long lotId, Long floorId) {}

    private Optional<FloorContext> resolveFloorContext(ParkingSpace space) {
        Section section = space.getSection();
        if (section == null) return Optional.empty();

        Floor floor = section.getFloor();
        if (floor == null) return Optional.empty();

        ParkingLot lot = floor.getParkingLot();
        if (lot == null) return Optional.empty();

        return Optional.of(new FloorContext(lot.getId(), floor.getId()));
    }
}

