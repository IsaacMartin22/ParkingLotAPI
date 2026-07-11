package apiservice.controller;

import parkinglot.common.response.ParkingSpaceResponse;
import apiservice.mapper.ParkingSpaceMapper;
import apiservice.service.ParkingSpaceService;
import parkinglot.common.request.ParkingSpaceUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService service;

    public ParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParkingSpaceResponse> getAll() {
        return service.findAll().stream()
                .map(ParkingSpaceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(space -> ResponseEntity.ok(ParkingSpaceMapper.toResponse(space)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> update(@PathVariable Long id, @RequestBody ParkingSpaceUpdateRequest request) {
        return service.findById(id)
                .map(existing -> {
                    if (existing.getLicensePlate() != null) {
                        log.error("Parking space must be empty before adding a new car to it. Space ID: {}, License Plate: {}", existing.getId(), existing.getLicensePlate());
                        return ResponseEntity.status(409).body(ParkingSpaceMapper.toResponse(existing));
                    }
                    else {
                        existing.setColor(request.getColor());
                        existing.setMake(request.getMake());
                        existing.setModel(request.getModel());
                        existing.setManufacturingYear(request.getManufacturingYear());
                        existing.setLicensePlate(request.getLicensePlate());
                    }
                    return ResponseEntity.ok(ParkingSpaceMapper.toResponse(service.save(existing)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> removeCar(@PathVariable Long id) {
        return service.findById(id)
                .map(existing -> {
                    if (existing.getLicensePlate() == null) {
                        log.error("Trying to remove a car from an empty parking space with id={}", id);
                        return ResponseEntity.status(409).body(ParkingSpaceMapper.toResponse(existing));
                    }
                    else {
                        existing.setColor(null);
                        existing.setMake(null);
                        existing.setModel(null);
                        existing.setManufacturingYear(null);
                        existing.setLicensePlate(null);
                        return ResponseEntity.ok(ParkingSpaceMapper.toResponse(service.removeCar(existing)));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
