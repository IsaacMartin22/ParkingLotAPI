package apiservice.repository;

import apiservice.dbentity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import parkinglot.common.model.AnalyticsEventTypes;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    @Query(value = """
            SELECT *
            FROM analytics
            ORDER BY id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Analytics> getAll(@Param("offset") int offset, @Param("limit") int limit);

    @Query(value = """
            SELECT *
            FROM analytics
            WHERE event_type = :eventType
            ORDER BY id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Analytics> getByEventTypeValue(@Param("eventType") String eventType, @Param("offset") int offset, @Param("limit") int limit);

    default List<Analytics> getByEventType(AnalyticsEventTypes eventType, int offset, int limit) {
        return getByEventTypeValue(eventType.name(), offset, limit);
    }
}
