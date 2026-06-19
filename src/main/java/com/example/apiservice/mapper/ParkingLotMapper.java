package com.example.apiservice.mapper;

import com.example.apiservice.pojo.ParkingLotFloorSummaryResponse;
import com.example.apiservice.pojo.ParkingLotResponse;
import com.example.apiservice.dbentity.Floor;
import com.example.apiservice.dbentity.ParkingLot;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ParkingLotMapper {
    private static final Map<Long, CacheEntry> capacityCache = new ConcurrentHashMap<>();
    private static final Map<Long, CacheEntry> freeSpacesCache = new ConcurrentHashMap<>();

    private static final long CAPACITY_EXPIRATION_MS = 24L * 60 * 60 * 1000; // 1 day
    private static final long FREE_SPACES_EXPIRATION_MS = 30 * 1000; // 30 seconds

    private static class CacheEntry {
        final int value;
        final long expiryMillis;

        CacheEntry(int value, long expiryMillis) {
            this.value = value;
            this.expiryMillis = expiryMillis;
        }
    }

    public static ParkingLotResponse toResponse(ParkingLot lot) {
        if (lot == null) {
            return null;
        }

        List<ParkingLotFloorSummaryResponse> floorSummaries = lot.getFloors() != null
            ? lot.getFloors().stream()
                .map(ParkingLotMapper::toFloorSummary)
                .collect(Collectors.toList())
            : Collections.emptyList();

        int totalCapacity;
        int totalFreeSpaces;

        Long lotId = lot.getId();
        long now = System.currentTimeMillis();

        if (lotId != null) {
            // Capacity cache
            CacheEntry capEntry = capacityCache.get(lotId);
            if (capEntry != null && capEntry.expiryMillis > now) {
                totalCapacity = capEntry.value;
            } else {
                totalCapacity = computeCapacity(lot);
                capacityCache.put(lotId, new CacheEntry(totalCapacity, now + CAPACITY_EXPIRATION_MS));
            }

            // Free spaces cache
            CacheEntry freeEntry = freeSpacesCache.get(lotId);
            if (freeEntry != null && freeEntry.expiryMillis > now) {
                totalFreeSpaces = freeEntry.value;
            } else {
                totalFreeSpaces = computeFreeSpaces(lot);
                freeSpacesCache.put(lotId, new CacheEntry(totalFreeSpaces, now + FREE_SPACES_EXPIRATION_MS));
            }
        } else {
            // no id available, compute on the fly (no caching)
            totalCapacity = computeCapacity(lot);
            totalFreeSpaces = computeFreeSpaces(lot);
        }

        return new ParkingLotResponse(
            lot.getId(),
            lot.getName(),
            lot.getAddress(),
            totalCapacity,
            totalFreeSpaces,
            lot.getType() != null ? lot.getType().toString() : null,
            lot.getFloors() != null
                ? lot.getFloors().stream()
                    .map(Floor::getId)
                    .collect(Collectors.toList())
                : Collections.emptyList(),
            floorSummaries
        );
    }

    private static ParkingLotFloorSummaryResponse toFloorSummary(Floor floor) {
        int capacity = computeFloorCapacity(floor);
        int freeSpaces = computeFloorFreeSpaces(floor);
        return new ParkingLotFloorSummaryResponse(
            floor.getId(),
            floor.getName(),
            capacity,
            freeSpaces
        );
    }

    private static int computeCapacity(ParkingLot lot) {
        int total = 0;
        if (lot.getFloors() != null) {
            for (var floor : lot.getFloors()) {
                total += computeFloorCapacity(floor);
            }
        }
        return total;
    }

    private static int computeFreeSpaces(ParkingLot lot) {
        int totalFree = 0;
        if (lot.getFloors() != null) {
            for (var floor : lot.getFloors()) {
                totalFree += computeFloorFreeSpaces(floor);
            }
        }
        return totalFree;
    }

    private static int computeFloorCapacity(Floor floor) {
        int total = 0;
        if (floor.getSections() != null) {
            for (var section : floor.getSections()) {
                if (section.getParkingSpaces() != null) {
                    total += section.getParkingSpaces().size();
                }
            }
        }
        return total;
    }

    private static int computeFloorFreeSpaces(Floor floor) {
        int totalFree = 0;
        if (floor.getSections() != null) {
            for (var section : floor.getSections()) {
                if (section.getParkingSpaces() != null) {
                    for (var space : section.getParkingSpaces()) {
                        if (space.getCar() == null) {
                            totalFree++;
                        }
                    }
                }
            }
        }
        return totalFree;
    }
}

