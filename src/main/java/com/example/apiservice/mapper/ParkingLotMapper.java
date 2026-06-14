package com.example.apiservice.mapper;

import com.example.apiservice.dto.ParkingLotResponse;
import com.example.apiservice.entity.Level;
import com.example.apiservice.entity.ParkingLot;
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
                    .map(Level::getId)
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList()
        );
    }

    private static int computeCapacity(ParkingLot lot) {
        int total = 0;
        if (lot.getFloors() != null) {
            for (var level : lot.getFloors()) {
                if (level.getSections() != null) {
                    for (var section : level.getSections()) {
                        if (section.getParkingSpaces() != null) {
                            total += section.getParkingSpaces().size();
                        }
                    }
                }
            }
        }
        return total;
    }

    private static int computeFreeSpaces(ParkingLot lot) {
        int totalFree = 0;
        if (lot.getFloors() != null) {
            for (var level : lot.getFloors()) {
                if (level.getSections() != null) {
                    for (var section : level.getSections()) {
                        if (section.getParkingSpaces() != null) {
                            for (var space : section.getParkingSpaces()) {
                                if (space.getCar() == null) {
                                    totalFree++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return totalFree;
    }
}

