package com.example.apiservice.repository;

import com.example.apiservice.dbentity.ParkingLot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "floors",
            "floors.sections",
            "floors.sections.parkingSpaces",
            "floors.sections.parkingSpaces.car"
    })
    List<ParkingLot> findAll();

    @Override
    @EntityGraph(attributePaths = {
            "floors",
            "floors.sections",
            "floors.sections.parkingSpaces",
            "floors.sections.parkingSpaces.car"
    })
    Optional<ParkingLot> findById(Long id);
}
