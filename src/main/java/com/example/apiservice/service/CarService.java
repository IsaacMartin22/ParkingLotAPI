package com.example.apiservice.service;

import com.example.apiservice.pojo.CarEvent;
import com.example.apiservice.pojo.CarEventType;
import com.example.apiservice.pojo.CarResponse;
import com.example.apiservice.dbentity.Car;
import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.ParkingLot;
import com.example.apiservice.dbentity.ParkingSpace;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.mapper.CarMapper;
import com.example.apiservice.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final FloorEventService floorEventService;

    public CarService(CarRepository carRepository, FloorEventService floorEventService) {
        this.carRepository = carRepository;
        this.floorEventService = floorEventService;
    }

    public List<Car> findAll() {
        return carRepository.findAll();
    }

    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    public Car save(Car car) {
        boolean isNew = (car.getId() == null);
        Car saved = carRepository.save(car);

        // Re-fetch to ensure all associations (ParkingSpace → Section → Floor → ParkingLot) are loaded
        Car fullCar = carRepository.findById(saved.getId()).orElse(saved);

        resolveFloorContext(fullCar).ifPresent(ctx -> {
            CarEventType type = isNew ? CarEventType.ADD : CarEventType.UPDATE;
            CarResponse carResponse = CarMapper.toResponse(fullCar);
            CarEvent event = new CarEvent(type, ctx.lotId, ctx.floorId, ctx.spaceId, carResponse, Instant.now());
            floorEventService.publishEvent(ctx.lotId, ctx.floorId, event);
        });

        return saved;
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private record FloorContext(Long lotId, Long floorId, Long spaceId) {}

    private Optional<FloorContext> resolveFloorContext(Car car) {
        ParkingSpace space = car.getParkingSpace();
        if (space == null) return Optional.empty();

        Section section = space.getSection();
        if (section == null) return Optional.empty();

        Floor floor = section.getFloor();
        if (floor == null) return Optional.empty();

        ParkingLot lot = floor.getParkingLot();
        if (lot == null) return Optional.empty();

        return Optional.of(new FloorContext(lot.getId(), floor.getId(), space.getId()));
    }
}
