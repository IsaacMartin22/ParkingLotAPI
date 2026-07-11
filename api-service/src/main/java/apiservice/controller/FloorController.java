package apiservice.controller;

import apiservice.mapper.FloorMapper;
import apiservice.service.FloorService;
import parkinglot.common.response.FloorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/floors")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping
    public List<FloorResponse> getAllFloors() {
        return floorService.getAllFloors().stream()
                .map(FloorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FloorResponse> getFloorById(@PathVariable Long id) {
        return floorService.getFloorById(id)
                .map(floor -> ResponseEntity.ok(FloorMapper.toResponse(floor)))
                .orElse(ResponseEntity.notFound().build());
    }
}
