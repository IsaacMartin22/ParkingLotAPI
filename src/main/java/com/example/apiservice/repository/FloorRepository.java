package com.example.apiservice.repository;

import com.example.apiservice.dbentity.Floor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {

    @EntityGraph(attributePaths = {
            "parkingLot",
            "sections",
            "sections.parkingSpaces",
            "sections.parkingSpaces.car"
    })
    Optional<Floor> findWithSectionsAndParkingSpacesById(Long id);
}

