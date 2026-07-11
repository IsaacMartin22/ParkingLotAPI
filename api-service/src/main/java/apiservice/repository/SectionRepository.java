package apiservice.repository;

import apiservice.dbentity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    long countByFloor_Id(Long floorId);
}
