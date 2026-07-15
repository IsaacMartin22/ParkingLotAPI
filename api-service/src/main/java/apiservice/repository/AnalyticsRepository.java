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
            ORDER BY
                CASE WHEN :sortField = 'eventType' AND :sortDirection = 'asc' THEN event_type END ASC,
                CASE WHEN :sortField = 'eventType' AND :sortDirection = 'desc' THEN event_type END DESC,
                CASE WHEN :sortField = 'currentUrl' AND :sortDirection = 'asc' THEN current_url END ASC,
                CASE WHEN :sortField = 'currentUrl' AND :sortDirection = 'desc' THEN current_url END DESC,
                CASE WHEN :sortField = 'browser' AND :sortDirection = 'asc' THEN browser END ASC,
                CASE WHEN :sortField = 'browser' AND :sortDirection = 'desc' THEN browser END DESC,
                CASE WHEN :sortField = 'operatingSystem' AND :sortDirection = 'asc' THEN operating_system END ASC,
                CASE WHEN :sortField = 'operatingSystem' AND :sortDirection = 'desc' THEN operating_system END DESC,
                CASE WHEN :sortField = 'sessionId' AND :sortDirection = 'asc' THEN session_id END ASC,
                CASE WHEN :sortField = 'sessionId' AND :sortDirection = 'desc' THEN session_id END DESC,
                CASE WHEN :sortField = 'ipAddress' AND :sortDirection = 'asc' THEN ip_address END ASC,
                CASE WHEN :sortField = 'ipAddress' AND :sortDirection = 'desc' THEN ip_address END DESC,
                CASE WHEN :sortField = 'timestamp' AND :sortDirection = 'asc' THEN "timestamp" END ASC,
                CASE WHEN :sortField = 'timestamp' AND :sortDirection = 'desc' THEN "timestamp" END DESC,
                id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Analytics> getAll(@Param("offset") int offset, @Param("limit") int limit, @Param("sortField") String sortField, @Param("sortDirection") String sortDirection);

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
