package apiservice.repository;

import apiservice.dbentity.ParkingSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    long countBySection_Id(Long sectionId);
}
