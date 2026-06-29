package com.example.apiservice.service;

import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.mapper.CarMapper;
import com.example.apiservice.pojo.CarEvent;
import com.example.apiservice.pojo.CarEventType;
import com.example.apiservice.pojo.CarResponse;
import com.example.apiservice.repository.ParkingSpaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ParkingSpaceService {

    private static final int MAX_SPACES_PER_SECTION = 10;

    private final ParkingSpaceRepository repo;
    private final FloorEventService floorEventService;
    private final FloorContextResolver floorContextResolver;

    public ParkingSpaceService(
            ParkingSpaceRepository repo,
            FloorEventService floorEventService,
            FloorContextResolver floorContextResolver
    ) {
        this.repo = repo;
        this.floorEventService = floorEventService;
        this.floorContextResolver = floorContextResolver;
    }

    public List<ParkingSpace> findAll() {
        return repo.findAll();
    }

    public Optional<ParkingSpace> findById(Long id) {
        return repo.findById(id);
    }

    public ParkingSpace save(ParkingSpace space) {
        ParkingSpace existingSpace = null;
        if (space.getId() != null) {
            existingSpace = repo.findById(space.getId()).orElse(null);
        }

        validateSectionCapacity(space, existingSpace);

        boolean isUpdate = existingSpace != null;
        ParkingSpace saved = repo.save(space);

        if (!isUpdate) {
            return saved;
        }

        ParkingSpace fullSpace = repo.findById(saved.getId()).orElse(saved);
        floorContextResolver.resolve(fullSpace).ifPresent(ctx -> {
            CarResponse carResponse = CarMapper.toResponse(fullSpace.getCar());
            CarEvent event = new CarEvent(
                    CarEventType.UPDATE,
                    ctx.lotId(),
                    ctx.floorId(),
                    ctx.spaceId(),
                    carResponse,
                    Instant.now()
            );
            floorEventService.publishEvent(ctx.lotId(), ctx.floorId(), event);
        });

        return saved;
    }

    private void validateSectionCapacity(ParkingSpace incomingSpace, ParkingSpace existingSpace) {
        if (incomingSpace.getSection() == null || incomingSpace.getSection().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parking space must reference a section ID");
        }

        Long targetSectionId = incomingSpace.getSection().getId();

        if (existingSpace != null) {
            Long currentSectionId = existingSpace.getSection() == null
                    ? null
                    : existingSpace.getSection().getId();
            if (targetSectionId.equals(currentSectionId)) {
                return;
            }
        }

        long spaceCount = repo.countBySection_Id(targetSectionId);
        if (spaceCount >= MAX_SPACES_PER_SECTION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section cannot have more than " + MAX_SPACES_PER_SECTION + " parking spaces"
            );
        }
    }
}
