package com.example.apiservice.repository;

import com.example.apiservice.entity.Level;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {

    @EntityGraph(attributePaths = {
            "parkingLot",
            "sections",
            "sections.parkingSpaces",
            "sections.parkingSpaces.car"
    })
    Optional<Level> findWithSectionsAndParkingSpacesById(Long id);
}
