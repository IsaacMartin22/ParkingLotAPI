package apiservice.controller;

import apiservice.service.FloorEventService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/lots/{lotId}/floors/{floorId}/events")
public class FloorEventsController {

    private final FloorEventService floorEventService;

    public FloorEventsController(FloorEventService floorEventService) {
        this.floorEventService = floorEventService;
    }

    /**
     * Subscribe to real-time car events for a specific floor in a parking lot.
     * <p>
     * Events emitted:
     * <ul>
     *   <li>{@code UPDATE} - a car on this floor was updated</li>
     *   <li>{@code REMOVE} - a car was removed from a parking space on this floor</li>
     * </ul>
     *
     * @param lotId   the parking lot ID
     * @param floorId the floor ID
     * @return an SSE stream
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long lotId, @PathVariable Long floorId) {
        return floorEventService.subscribe(lotId, floorId);
    }
}
