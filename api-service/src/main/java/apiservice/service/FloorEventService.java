package apiservice.service;

import parkinglot.common.model.ParkingSpaceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FloorEventService {

    private static final Logger log = LoggerFactory.getLogger(FloorEventService.class);

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public FloorEventService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private String key(Long lotId, Long floorId) {
        return lotId + ":" + floorId;
    }

    /**
     * Subscribe a client to SSE events for a specific lot/floor combination.
     */
    public SseEmitter subscribe(Long lotId, Long floorId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = no timeout
        String key = key(lotId, floorId);
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));

        try {
            Map<String, Object> connectedPayload = new LinkedHashMap<>();
            connectedPayload.put("status", "subscribed");
            connectedPayload.put("lotId", lotId);
            connectedPayload.put("floorId", floorId);
            emitter.send(SseEmitter.event()
                    .data(connectedPayload));
        } catch (IOException e) {
            removeEmitter(key, emitter);
            emitter.completeWithError(e);
            return emitter;
        }

        log.debug("New SSE subscriber for lot={} floor={}, total subscribers: {}",
                lotId, floorId, emitters.get(key).size());
        return emitter;
    }

    /**
     * Broadcast a CarEvent to all subscribers of the given lot/floor.
     */
    public void publishEvent(Long lotId, Long floorId, ParkingSpaceEvent event) {
        String key = key(lotId, floorId);
        CopyOnWriteArrayList<SseEmitter> floorEmitters = emitters.get(key);
        if (floorEmitters == null || floorEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : floorEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(event)));
            } catch (IOException e) {
                log.debug("Removing dead SSE emitter for lot={} floor={}", lotId, floorId);
                deadEmitters.add(emitter);
            }
        }
        floorEmitters.removeAll(deadEmitters);
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> floorEmitters = emitters.get(key);
        if (floorEmitters != null) {
            floorEmitters.remove(emitter);
        }
    }
}
