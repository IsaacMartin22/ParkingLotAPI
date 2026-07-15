package apiservice.service;

import apiservice.dbentity.ParkingSpace;
import parkinglot.common.model.ParkingSpaceEvent;
import parkinglot.common.model.ParkingSpaceEventType;
import parkinglot.common.response.ParkingSpaceResponse;
import apiservice.repository.ParkingSpaceRepository;
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
        boolean wasOccupied = existingSpace != null && existingSpace.getLicensePlate() != null;

        validateSectionCapacity(space, existingSpace);

        boolean isUpdate = existingSpace != null;
        ParkingSpace saved = repo.save(space);

        if (!isUpdate) {
            return saved;
        }

        ParkingSpace fullSpace = repo.findById(saved.getId()).orElse(saved);
        floorContextResolver.resolve(fullSpace).ifPresent(ctx -> {
            // Build a ParkingSpaceResponse from embedded parking space fields
            ParkingSpaceResponse parkingSpaceResponse = new ParkingSpaceResponse(
                    fullSpace.getId(),
                    fullSpace.getNumber(),
                    fullSpace.getLicensePlate() != null,
                    fullSpace.getSection().getId(),
                    fullSpace.getColor(),
                    fullSpace.getMake(),
                    fullSpace.getModel(),
                    fullSpace.getManufacturingYear() == null ? 0 : fullSpace.getManufacturingYear(),
                    fullSpace.getLicensePlate()
            );
            boolean isNowOccupied = fullSpace.getLicensePlate() != null;
            ParkingSpaceEventType eventType = !wasOccupied && isNowOccupied
                    ? ParkingSpaceEventType.ADD
                    : ParkingSpaceEventType.UPDATE;
            ParkingSpaceEvent event = new ParkingSpaceEvent(
                    eventType,
                    ctx.lotId(),
                    ctx.floorId(),
                    ctx.spaceId(),
                    parkingSpaceResponse,
                    Instant.now()
            );
            floorEventService.publishEvent(ctx.lotId(), ctx.floorId(), event);
        });

        return saved;
    }

    public ParkingSpace removeCar(ParkingSpace space) {
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
            // Build a ParkingSpaceResponse from embedded parking space fields
            ParkingSpaceResponse parkingSpaceResponse = new ParkingSpaceResponse(
                    fullSpace.getId(),
                    fullSpace.getNumber(),
                    fullSpace.getLicensePlate() != null,
                    fullSpace.getSection().getId(),
                    null,
                    null,
                    null,
                    0,
                    null
            );
            ParkingSpaceEvent event = new ParkingSpaceEvent(
                    ParkingSpaceEventType.REMOVE,
                    ctx.lotId(),
                    ctx.floorId(),
                    ctx.spaceId(),
                    parkingSpaceResponse,
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
