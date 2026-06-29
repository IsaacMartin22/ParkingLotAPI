package com.example.apiservice.service;

import com.example.apiservice.pojo.CarEvent;
import com.example.apiservice.pojo.CarEventType;
import com.example.apiservice.pojo.CarResponse;
import com.example.apiservice.dbentity.Car;
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
    private final FloorContextResolver floorContextResolver;

    public CarService(
            CarRepository carRepository,
            FloorEventService floorEventService,
            FloorContextResolver floorContextResolver
    ) {
        this.carRepository = carRepository;
        this.floorEventService = floorEventService;
        this.floorContextResolver = floorContextResolver;
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

        // Re-fetch to ensure all associations (ParkingSpace -> Section -> Floor -> ParkingLot) are loaded
        Car fullCar = carRepository.findById(saved.getId()).orElse(saved);

        floorContextResolver.resolve(fullCar).ifPresent(ctx -> {
            CarEventType type = isNew ? CarEventType.ADD : CarEventType.UPDATE;
            CarResponse carResponse = CarMapper.toResponse(fullCar);
            CarEvent event = new CarEvent(type, ctx.lotId(), ctx.floorId(), ctx.spaceId(), carResponse, Instant.now());
            floorEventService.publishEvent(ctx.lotId(), ctx.floorId(), event);
        });

        return saved;
    }
}
